package info.jonclark.treegraft.core.forestunpacking.pruning;

import info.jonclark.treegraft.core.forestunpacking.parses.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.scoring.ParseScorer;
import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.transduction.Transducer;

import java.util.ArrayList;
import java.util.List;

public class CrossProductParsePruner<R extends GrammarRule<T>, T extends Token> implements
		ParsePruner<R, T> {

	public CrossProductParsePruner() {
	}

	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			ParseScorer<R, T> scorer, Transducer<R, T> transducer) {

		List<Parse<T>> currentResultParses = new ArrayList<Parse<T>>();
		T sourceLhs = parentRule.getLhs();
		T[] sourceRhs = parentRule.getRhs();
		T targetLhs = transducer.transduceLhs(parentRule);
		T[] targetRhs = transducer.transduceRhs(parentRule);
		Parse<T> seedBlankParse = new Parse<T>(sourceLhs, targetLhs, sourceRhs, targetRhs);
		currentResultParses.add(seedBlankParse);

		int[] targetToSourceAlignment = transducer.getTargetToSourceRhsAlignment(parentRule);

		// take the crossproduct of the current parse with the new parses
		for (int targetRhsIndex = 0; targetRhsIndex < parses.length; targetRhsIndex++) {

			int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
			List<Parse<T>> parseList = parses[targetRhsIndex];

			currentResultParses =
					combine(scorer, sourceRhsIndex, targetRhsIndex, currentResultParses, parseList);
		}

		return currentResultParses;
	}

	private List<Parse<T>> combine(ParseScorer<R, T> scorer, int sourceRhsIndex,
			int targetRhsIndex, List<Parse<T>> currentParsesToBeCombined,
			List<Parse<T>> rightParsesToBeCombined) {

		List<Parse<T>> combinedParses = new ArrayList<Parse<T>>();

		for (Parse<T> parseFromBackpointer : rightParsesToBeCombined) {
			for (Parse<T> resultParse : currentParsesToBeCombined) {

				Parse<T> expandedParse = new Parse<T>(resultParse);
				expandedParse.appendParse(sourceRhsIndex, targetRhsIndex,
						parseFromBackpointer);

				double newScore =
						scorer.combineChildScores(parseFromBackpointer.getLogProb(),
								resultParse.getLogProb());

				Scores dummyScores = new Scores(newScore);
				expandedParse.setCurrentScore(dummyScores);
				combinedParses.add(expandedParse);
			}
		}

		return combinedParses;
	}
}
