package info.jonclark.treegraft.core.mergingX;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeamSearchParsePruner<R extends GrammarRule<T>, T extends Token> implements
		ParsePruner<R, T> {

	private final TokenFactory<T> tokenFactory;
	private final int beamSize;

	public BeamSearchParsePruner(TokenFactory<T> tokenFactory, int beamSize) {
		this.tokenFactory = tokenFactory;
		this.beamSize = beamSize;
	}

	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			Scorer<R, T> scorer, Transducer<R, T> transducer) {

		// TODO: Move recombination here

		List<Parse<T>> currentResultParses = new Beam<Parse<T>>(beamSize);
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

		List<Parse<T>> combinedParses = new Beam<Parse<T>>(beamSize);

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

		HashMap<TokenSequence<T>, DecoderHypothesis<T>> uniqueHypotheses =
				new HashMap<TokenSequence<T>, DecoderHypothesis<T>>();

		// TODO: Apply cube pruning right here
		for (DecoderHypothesis<T> hyp1 : beam1) {
			for (DecoderHypothesis<T> hyp2 : beam2) {

				ArrayList<T> combinedTokens =
						new ArrayList<T>(hyp1.getTokens().size() + hyp2.getTokens().size());
				combinedTokens.addAll(hyp1.getTokens());
				combinedTokens.addAll(hyp2.getTokens());

				TokenSequence<T> combinedTokenSequence =
						tokenFactory.makeTokenSequence(combinedTokens);

				DecoderHypothesis<T> previousHypothesisWithSameYield =
						uniqueHypotheses.get(combinedTokenSequence);

				if (previousHypothesisWithSameYield == null) {
					FeatureScores combinedScore =
							scorer.combineHypotheses(hyp1, hyp2, combinedTokenSequence);

					ArrayList<Parse<T>> parses =
							new ArrayList<Parse<T>>(hyp1.getParses().size()
									+ hyp2.getParses().size());
					parses.addAll(hyp1.getParses());
					parses.addAll(hyp2.getParses());

					DecoderHypothesis<T> combinedHyp =
							new DecoderHypothesis<T>(parses, combinedTokens, combinedScore);

					outputBeam.add(combinedHyp);
					uniqueHypotheses.put(combinedTokenSequence, combinedHyp);
				} else {

					// if we actually make the new hypothesis and keep scoring
					// it individually, we waste lots of time, but throwing it
					// away seems wrong too

					System.err.println("DISCARDING PARSE!!! -- not summing!");
					// previousHypothesisWithSameYield.addChildHypothesis(qua);
				}
			}
		}

	}
}
