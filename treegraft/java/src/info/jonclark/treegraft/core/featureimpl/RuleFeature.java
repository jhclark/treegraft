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
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

@OptionsTarget(RuleFeature.RuleFeatureOptions.class)
public class RuleFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, RuleScore> {

	private double[] weights;
	private final ProfilerTimer ruleFeatureTimer;

	public static class RuleFeatureOptions implements Options {

		@Option(name = "features.rule.sgt.weight", usage = "Log-linear lambda weight for source-given-target rule scores")
		public double ruleSgtWeight;

		@Option(name = "features.rule.tgs.weight", usage = "Log-linear lambda weight for target-given-source rule scores")
		public double ruleTgsWeight;
	}

	// TODO: Make RuleScore an interface w/ .add and .recombine methods
	public RuleFeature(RuleFeatureOptions opts, TreegraftConfig<R, T> config) {
		this.weights = new double[] { opts.ruleSgtWeight, opts.ruleTgsWeight };
		this.ruleFeatureTimer =
				ProfilerTimer.newTimer("ruleFeature", config.profiler.featureTimer, true, false);
	}

	public RuleScore combineChildParseScores(PartialParse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, RuleScore accumulatedScore, PartialParse<T> addedChild,
			TokenSequence<T> addedSeq, RuleScore addedScore, TokenSequence<T> combinedSeq,
			List<T> inputSentence) {

		ruleFeatureTimer.go();
		double sgt = accumulatedScore.sgt + addedScore.sgt;
		double tgs = accumulatedScore.tgs + addedScore.tgs;
		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		RuleScore ruleScore = new RuleScore(sgt, tgs);
		ruleFeatureTimer.pause();
		return ruleScore;
	}

	public RuleScore combineHypotheses(DecoderHypothesis<T> hyp1, TokenSequence<T> tokensFromHyp1,
			RuleScore scoreFromHyp1, DecoderHypothesis<T> hyp2, TokenSequence<T> tokensFromHyp2,
			RuleScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence, List<T> inputSentence) {

		ruleFeatureTimer.go();

		double sgt = scoreFromHyp1.sgt + scoreFromHyp2.sgt;
		double tgs = scoreFromHyp1.tgs + scoreFromHyp2.tgs;
		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		RuleScore ruleScore = new RuleScore(sgt, tgs);

		ruleFeatureTimer.pause();
		return ruleScore;
	}

	public RuleScore combineRuleScoreWithChildren(PartialParse<T> parse, RuleScore parseScore,
			R ruleToAppend, List<T> inputSentence) {

		ruleFeatureTimer.go();

		// System.out.println("Using rule " + ruleToAppend.getRuleId() + ": " +
		// ruleToAppend.getRuleScores().sgt
		// + " " + ruleToAppend.getRuleScores().tgs);

		double sgt = parseScore.sgt + ruleToAppend.getRuleScores().sgt;
		double tgs = parseScore.tgs + ruleToAppend.getRuleScores().tgs;
		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		RuleScore ruleScore = new RuleScore(sgt, tgs);

		ruleFeatureTimer.pause();
		return ruleScore;
	}

	public RuleScore getInitialScore() {
		// use dummy scores
		return new RuleScore(0.0, 0.0);
	}

	public RuleScore scoreTerminalParse(PartialParse<T> terminalParse, TokenSequence<T> seq) {

		return getInitialScore();
	}

	/**
	 * This method is called ONLY IF the merger calls it. That is, the merger
	 * might choose to keep on the the best scoring hypothesis, making this
	 * method irrelevant.
	 */
	public RuleScore recombine(RuleScore a, RuleScore b) {
		double sgt = ProbUtils.sumInNonLogSpace(a.sgt, b.sgt);
		double tgs = ProbUtils.sumInNonLogSpace(a.tgs, b.tgs);

		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		return new RuleScore(sgt, tgs);
	}

	public String getFeatureName() {
		return "rule";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "sgt", "tgs" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}

	public void setFeatureWeightVector(double[] lambdas) {
		this.weights = lambdas;
	}
}
