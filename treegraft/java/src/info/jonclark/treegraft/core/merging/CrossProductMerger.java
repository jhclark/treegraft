package info.jonclark.treegraft.core.merging;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.List;

public class CrossProductMerger<R extends GrammarRule<T>, T extends Token> implements
		Merger<R, T> {

	public CrossProductMerger() {
	}

	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			Scorer<R, T> scorer, Transducer<R, T> transducer) {

		List<Parse<T>> currentResultParses = new ArrayList<Parse<T>>();
		T sourceLhs = parentRule.getLhs();
		T[] sourceRhs = parentRule.getRhs();
		T targetLhs = transducer.transduceLhs(parentRule);
		T[] targetRhs = transducer.transduceRhs(parentRule);
		Parse<T> seedBlankParse =
				new Parse<T>(sourceLhs, targetLhs, sourceRhs, targetRhs,
						scorer.getInitialFeatureScores());
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

	private List<Parse<T>> combine(Scorer<R, T> scorer, int sourceRhsIndex, int targetRhsIndex,
			List<Parse<T>> currentParsesToBeCombined, List<Parse<T>> rightParsesToBeCombined) {

		List<Parse<T>> combinedParses = new ArrayList<Parse<T>>();

		for (Parse<T> parseFromBackpointer : rightParsesToBeCombined) {
			for (Parse<T> resultParse : currentParsesToBeCombined) {

				Parse<T> expandedParse = new Parse<T>(resultParse);
				expandedParse.appendParse(sourceRhsIndex, targetRhsIndex, parseFromBackpointer);

				FeatureScores newScores =
						scorer.combineChildParseScores(parseFromBackpointer, resultParse);
				expandedParse.setCurrentScore(newScores);
				combinedParses.add(expandedParse);
			}
		}

		return combinedParses;
	}

	public void combineCrossProductOfHypotheses(Scorer<R, T> scorer,
			Beam<DecoderHypothesis<T>> beam1, Beam<DecoderHypothesis<T>> beam2,
			Beam<DecoderHypothesis<T>> outputBeam) {

		throw new Error("Unimplemented.");
	}
}
