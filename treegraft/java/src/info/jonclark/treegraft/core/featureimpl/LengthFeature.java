package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.ProbUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

@OptionsTarget(LengthFeature.LengthFeatureOptions.class)
public class LengthFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, LengthScore> {

	private final double expectedRatio;
	private double[] weights;
	private final ProfilerTimer lengthTimer;

	public static class LengthFeatureOptions implements Options {

		@Option(name = "features.length.weight", usage = "The log-linear lambda weight for the length ratio feature")
		public double lengthWeight;

		@Option(name = "features.length.ratio", usage = "The expected word ratio (??? make sure this direction is right) from source to target language (source/target) to be used in the length ratio feature")
		public double lengthRatio;
	}

	public LengthFeature(LengthFeatureOptions opts, TreegraftConfig<R, T> config) {
		this.expectedRatio = opts.lengthRatio;
		this.weights = new double[] { opts.lengthWeight };
		this.lengthTimer =
				ProfilerTimer.newTimer("length", config.profiler.featureTimer, true, false);
	}

	private LengthScore calcLengthPenalty(int srclength, int tgtlength) {

		double lengthPenalty;
		if (srclength == 0 || tgtlength == 0) {
			lengthPenalty = ProbUtils.FLOOR;
		} else {

			double actualRatio = (double) tgtlength / (double) srclength;

			double lengthBonus;
			if (actualRatio > expectedRatio) {
				lengthBonus = Math.exp(expectedRatio - actualRatio);
			} else if (actualRatio < expectedRatio) {
				lengthBonus = Math.exp(actualRatio - expectedRatio);
			} else {
				lengthBonus = 0.0;
			}

			if (lengthBonus > 0) {
				lengthPenalty = ProbUtils.logProb(lengthBonus);
			} else {
				lengthPenalty = ProbUtils.FLOOR;
			}
		}

		return new LengthScore(lengthPenalty);
	}

	public LengthScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, LengthScore accumulatedScore, Parse<T> addedChild,
			TokenSequence<T> addedSeq, LengthScore addedScore, TokenSequence<T> combinedSeq,
			List<T> inputSentence) {

		lengthTimer.go();
		// compensate for possibility of inserted target words
		// these will have start and end indices of -1
		int srclength1 = accumulatedParse.getEndIndex() - accumulatedParse.getStartIndex();
		int srclength2 = addedChild.getEndIndex() - addedChild.getStartIndex();

		int srclength = srclength1 + srclength2;
		int tgtlength = accumulatedSeq.size() + addedSeq.size();
		LengthScore lengthPenalty = calcLengthPenalty(srclength, tgtlength);
		lengthTimer.pause();

		return lengthPenalty;
	}

	public LengthScore combineHypotheses(DecoderHypothesis<T> hyp1,
			TokenSequence<T> tokensFromHyp1, LengthScore scoreFromHyp1, DecoderHypothesis<T> hyp2,
			TokenSequence<T> tokensFromHyp2, LengthScore scoreFromHyp2,
			TokenSequence<T> combinedTokenSequence, List<T> inputSentence) {

		lengthTimer.go();
		int srclength = hyp2.getSourceEndIndex() - hyp1.getSourceStartIndex();
		int tgtlength = tokensFromHyp1.size() + tokensFromHyp2.size();
		LengthScore lengthPenalty = calcLengthPenalty(srclength, tgtlength);
		lengthTimer.pause();

		return lengthPenalty;
	}

	public LengthScore combineRuleScoreWithChildren(Parse<T> parse, LengthScore parseScore,
			R ruleToAppend, List<T> inputSentence) {

		// no change in length -> no change to score
		return parseScore;
	}

	public String getFeatureName() {
		return "length";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "penalty" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}

	public void setFeatureWeightVector(double[] lambdas) {
		this.weights = lambdas;
	}

	public LengthScore getInitialScore() {
		return new LengthScore(0.0);
	}

	public LengthScore scoreTerminalParse(Parse<T> terminalParse, TokenSequence<T> seq) {

		// since it's not necessarily associated with a source token, we have no
		// way of scoring a single target-side token individually
		return new LengthScore(0.0);
	}

	public LengthScore recombine(LengthScore a, LengthScore b) {
		// these must be the same if we recombined
		return a;
	}

}
