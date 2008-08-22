package info.jonclark.treegraft.core.forestunpacking;

import info.jonclark.treegraft.core.merging.Merger;
import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.ActiveArc;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.List;

public class ForestUnpacker<R extends GrammarRule<T>, T extends Token> {

	private final Scorer<R, T> scorer;
	private final Merger<R, T> pruner;
	private final Transducer<R, T> transducer;

	// TODO: Pass parses from the left to the parses being created on the right
	// e.g. we can use these for language model context
	public ForestUnpacker(Scorer<R, T> scorer, Merger<R, T> merger, Transducer<R, T> transducer) {

		this.scorer = scorer;
		this.pruner = merger;
		this.transducer = transducer;
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

			if (transducedRhs[targetRhsIndex].isTerminal()) {
				parsesFromBackpointer[targetRhsIndex] =
						outputTargetTerminal(ruleToUnpack, transducedRhs[targetRhsIndex]);
			} else {

				int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
				List<Key<R, T>> backpointers = arcToUnpack.getBackpointers(sourceRhsIndex);
				assert backpointers != null : "null backpointer list at index " + sourceRhsIndex
						+ " for key " + key.toString();

				parsesFromBackpointer[targetRhsIndex] =
						outputNonterminal(key, ruleToUnpack, sourceRhsIndex, backpointers);
			}

		}

		List<Parse<T>> result =
				pruner.combineCrossProductOfParses(ruleToUnpack, parsesFromBackpointer, scorer,
						transducer);

		for (Parse<T> parse : result) {

			// update parse scores
			FeatureScores combinedScore = scorer.combineRuleScoreWithChildren(parse, ruleToUnpack);
			parse.setCurrentScore(combinedScore);

			// TODO: Do this when creating the "empty seed parse"
			T[] sourceRhs = ruleToUnpack.getRhs();
			for (int sourceRhsIndex = 0; sourceRhsIndex < sourceRhs.length; sourceRhsIndex++) {
				if (sourceRhs[sourceRhsIndex].isTerminal()) {
					parse.appendSourceTerminal(sourceRhsIndex, sourceRhs[sourceRhsIndex]);
				}
			}
		}

		return result;
	}

	private ArrayList<Parse<T>> outputTargetTerminal(R parentRule, T terminal) {
		FeatureScores terminalScore = scorer.scoreTerminalToken(terminal);
		Parse<T> terminalParse = new Parse<T>(terminal, false, terminalScore);

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
					}

				}
			}
		}

		return parsesFromNonterminal;
	}

}
