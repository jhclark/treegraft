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

@OptionsTarget(FragmentationFeature.FragmentationFeatureOptions.class)
public class FragmentationFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, FragmentationScore> {

	private double[] weights;
	private final ProfilerTimer fragTimer;

	public static class FragmentationFeatureOptions implements Options {

		@Option(name = "features.frag.weight", usage = "The log-linear lambda weight for the fragmentation penalty feature")
		public double fragWeight;
	}

	public FragmentationFeature(FragmentationFeatureOptions opts, TreegraftConfig<R, T> config) {
		this.weights = new double[] { opts.fragWeight };
		this.fragTimer = ProfilerTimer.newTimer("frag", config.profiler.featureTimer, true, false);
	}

	private FragmentationScore calcFragScore(int nFragments, int nSourceWords) {

		double fragPenalty = (double) (nFragments - 1) / nSourceWords;
		double fragBonus = 1.0 - fragPenalty;
		double logFragBonus;
		if (fragBonus > 0) {
			logFragBonus = ProbUtils.logProb(fragBonus);
		} else {
			logFragBonus = ProbUtils.FLOOR;
		}
		return new FragmentationScore(logFragBonus, nFragments, nSourceWords);
	}

	public FragmentationScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, FragmentationScore accumulatedScore,
			Parse<T> addedChild, TokenSequence<T> addedSeq, FragmentationScore addedScore,
			TokenSequence<T> combinedSeq, List<T> inputSentence) {

		// parses are always considered one piece
		return accumulatedScore;
	}

	public FragmentationScore combineHypotheses(DecoderHypothesis<T> hyp1,
			TokenSequence<T> tokensFromHyp1, FragmentationScore scoreFromHyp1,
			DecoderHypothesis<T> hyp2, TokenSequence<T> tokensFromHyp2,
			FragmentationScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence,
			List<T> inputSentence) {

		// add fragments from each decoder hypothesis
		fragTimer.go();
		FragmentationScore fragmentationScore =
				calcFragScore(scoreFromHyp1.fragments + scoreFromHyp2.fragments,
						scoreFromHyp1.sourceWords + scoreFromHyp2.sourceWords);
		fragTimer.pause();
		return fragmentationScore;
	}

	public FragmentationScore combineRuleScoreWithChildren(Parse<T> parse,
			FragmentationScore parseScore, R ruleToAppend, List<T> inputSentence) {

		// parses are always considered one piece
		return parseScore;
	}

	public FragmentationScore getInitialScore() {
		// start with one piece
		return calcFragScore(1, 1);
	}

	public FragmentationScore scoreTerminalParse(Parse<T> terminalParse, TokenSequence<T> seq) {

		return getInitialScore();
	}

	public String getFeatureName() {
		return "frag";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "score" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}

	public void setFeatureWeightVector(double[] lambdas) {
		this.weights = lambdas;
	}

	public FragmentationScore recombine(FragmentationScore a, FragmentationScore b) {
		// TODO: Pass in the overall best choice to allow each feature to just
		// choose the feature of the overall best hypothesis

		// return the more optimistic score
		if (a.logFragBonus > b.logFragBonus) {
			return a;
		} else {
			return b;
		}
	}
}
