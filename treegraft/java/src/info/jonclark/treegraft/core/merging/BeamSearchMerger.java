package info.jonclark.treegraft.core.merging;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeamSearchMerger<R extends GrammarRule<T>, T extends Token> implements Merger<R, T> {

	private final TokenFactory<T> tokenFactory;
	private final int beamSize;
	private final boolean doParseYieldRecombination;
	private final boolean doHypothesisRecombination;

	public BeamSearchMerger(TokenFactory<T> tokenFactory, int beamSize,
			boolean doParseYieldRecombination, boolean doHypothesisRecombination) {

		this.tokenFactory = tokenFactory;
		this.beamSize = beamSize;
		this.doParseYieldRecombination = doParseYieldRecombination;
		this.doHypothesisRecombination = doHypothesisRecombination;
	}

	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			Scorer<R, T> scorer, Transducer<R, T> transducer) {

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

		HashMap<TokenSequence<T>, Parse<T>> uniqueParses =
				new HashMap<TokenSequence<T>, Parse<T>>();

		for (Parse<T> parseFromBackpointer : rightParsesToBeCombined) {
			for (Parse<T> resultParse : currentParsesToBeCombined) {

				Parse<T> expandedParse = new Parse<T>(resultParse);
				expandedParse.appendParse(sourceRhsIndex, targetRhsIndex, parseFromBackpointer);

				TokenSequence<T> combinedSeq =
						tokenFactory.makeTokenSequence(expandedParse.getTargetTokens());
				Parse<T> previousParseWithSameYield = null;

				if (doParseYieldRecombination)
					previousParseWithSameYield = uniqueParses.get(combinedSeq);

				if (previousParseWithSameYield == null) {

					FeatureScores newScores =
							scorer.combineChildParseScores(parseFromBackpointer, resultParse);
					expandedParse.setCurrentScore(newScores);
					combinedParses.add(expandedParse);

					if (doParseYieldRecombination)
						uniqueParses.put(combinedSeq, expandedParse);
				} else {
					FeatureScores recombinedScores = scorer.recombineParses(previousParseWithSameYield, expandedParse);
					previousParseWithSameYield.setCurrentScore(recombinedScores);
					previousParseWithSameYield.addRecombinedParse(expandedParse);
				}
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

				DecoderHypothesis<T> previousHypothesisWithSameYield = null;

				if (doHypothesisRecombination)
					previousHypothesisWithSameYield = uniqueHypotheses.get(combinedTokenSequence);

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

					if (doHypothesisRecombination)
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
