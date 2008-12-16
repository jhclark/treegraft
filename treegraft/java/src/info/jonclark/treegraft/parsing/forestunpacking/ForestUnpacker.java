package info.jonclark.treegraft.parsing.forestunpacking;

import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.ActiveArc;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.List;

@OptionsTarget(ForestUnpacker.ForestUnpackerOptions.class)
public class ForestUnpacker<R extends GrammarRule<T>, T extends Token> {

	private final Scorer<R, T> scorer;
	private final Merger<R, T> pruner;
	private final Transducer<R, T> transducer;
	private final ParseFactory<R, T> parseFactory;
	private final List<T> sourceInputTokens;

	public static class ForestUnpackerOptions implements Options {

	}

	// TODO: Pass parses from the left to the parses being created on the right
	// e.g. we can use these for language model context
	public ForestUnpacker(ForestUnpackerOptions opts, TreegraftConfig<R, T> config,
			ParseFactory<R, T> parseFactory, List<T> sourceInputTokens) {

		this.scorer = config.scorer;
		this.pruner = config.merger;
		this.transducer = config.ruleFactory.getTransducer();
		this.parseFactory = parseFactory;
		this.sourceInputTokens = sourceInputTokens;
	}

	/**
	 * Gets the partial (or possibly complete) parses associated with this
	 * <code>Key</code> as formatted by the <code>ParseFormatter</code>.
	 * 
	 * @param formatter
	 *            an object that determines how the resulting parses will look
	 *            (including whether they are source trees, target trees, or
	 *            target strings).
	 * @return an array of formatted parses
	 */
	public List<Parse<T>> getPartialParses(Key<R, T> key) {

		ArrayList<Parse<T>> result = new ArrayList<Parse<T>>();
		for (ActiveArc<R, T> arc : key.getActiveArcs()) {
			for (R rule : arc.getRules()) {
				result.addAll(unpackNonterminalBackpointers(key, arc, rule));
			}
		}
		return result;
	}

	private List<Parse<T>> unpackNonterminalBackpointers(Key<R, T> key,
			ActiveArc<R, T> arcToUnpack, R ruleToUnpack) {

		// TODO: Figure out how to safely cache
		// if(key.getTransducedParseCache() != null) {
		// return key.getTransducedParseCache();
		// }

		// iterate over all of the RHS constituents for this key
		int[] targetToSourceAlignment = transducer.getTargetToSourceRhsAlignment(ruleToUnpack);
		T[] transducedRhs = transducer.transduceRhs(ruleToUnpack);
		ArrayList<Parse<T>>[] parsesFromBackpointer = new ArrayList[transducedRhs.length];

		// traverse left to right across transduced RHS
		// when we find a non-terminal, we will use the alignment
		// to map it back to the backpointers, which we can then
		// use to continue descending down the source-side backbone
		// structure

		for (int targetRhsIndex = 0; targetRhsIndex < transducedRhs.length; targetRhsIndex++) {

			int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
			if (transducedRhs[targetRhsIndex].isTerminal()) {

				parsesFromBackpointer[targetRhsIndex] =
						outputTargetTerminal(key.getStartIndex(), key.getEndIndex(), ruleToUnpack,
								transducedRhs[targetRhsIndex], sourceRhsIndex, targetRhsIndex);
			} else {

				List<Key<R, T>> backpointers = arcToUnpack.getBackpointers(sourceRhsIndex);
				assert backpointers != null : "null backpointer list at index " + sourceRhsIndex
						+ " for key " + key.toString();

				parsesFromBackpointer[targetRhsIndex] =
						outputNonterminal(key, ruleToUnpack, sourceRhsIndex, backpointers);
			}

		}

		List<Parse<T>> result =
				pruner.combineCrossProductOfParses(ruleToUnpack, sourceInputTokens,
						key.getStartIndex(), key.getEndIndex(), parsesFromBackpointer, scorer,
						transducer, sourceInputTokens, parseFactory);

		for (Parse<T> parse : result) {

			// update parse scores
			FeatureScores combinedScore =
					scorer.combineRuleScoreWithChildren(parse, ruleToUnpack, sourceInputTokens);
			parse.setCurrentScore(combinedScore);
		}

		key.setTransducedParseCache(result);

		return result;
	}

	private ArrayList<Parse<T>> outputTargetTerminal(int sourceTokenStart, int sourceTokenEnd,
			R parentRule, T terminal, int sourceRhsIndex, int targetRhsIndex) {

		// TODO: MAKE SURE ONLY THE **FIRST/LAST** TERMINAL GETS MARKERS ADDED

		Parse<T> terminalParse =
				parseFactory.createParse(sourceTokenStart, sourceTokenEnd, sourceInputTokens,
						terminal, parentRule, targetRhsIndex);
		FeatureScores terminalScore = scorer.scoreTerminalParse(terminalParse);
		terminalParse.setCurrentScore(terminalScore);

		// System.out.println("TRANSDUCED TERMINAL: " +
		// terminalParse.toString());

		ArrayList<Parse<T>> list = new ArrayList<Parse<T>>(1);
		list.add(terminalParse);
		return list;
	}

	private ArrayList<Parse<T>> outputNonterminal(Key<R, T> key, R parentRule, int sourceRhsIndex,
			List<Key<R, T>> nonterminalBackpointers) {

		ArrayList<Parse<T>> parsesFromNonterminal = new ArrayList<Parse<T>>();

		// since each backpointer is guaranteed to satisfy for the constraints
		// only of AT LEAST one rule, we must check to make sure that this
		// backpointer satisfies the constraints of the current rule before
		// adding it
		for (Key<R, T> backpointer : nonterminalBackpointers) {
			for (ActiveArc<R, T> backpointerArc : backpointer.getActiveArcs()) {
				for (R backpointerRule : backpointerArc.getRules()) {

					if (parentRule.areConstraintsSatisfied(sourceRhsIndex, backpointerRule)) {

						List<Parse<T>> parsesFromBackpointer =
								unpackNonterminalBackpointers(backpointer, backpointerArc,
										backpointerRule);
						parsesFromNonterminal.addAll(parsesFromBackpointer);

						// for (Parse<T> p : parsesFromBackpointer) {
						// System.out.println("TRANSDUCED: " + p.toString());
						// }
					}

				}
			}
		}

		return parsesFromNonterminal;
	}

}
