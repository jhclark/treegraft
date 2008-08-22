package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public class RuleFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, RuleScore> {

	private final double[] weights;

	// TODO: Make RuleScore an interface w/ .add and .recombine methods
	public RuleFeature(double weight) {
		this.weights = new double[] { weight };
	}

	public RuleScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, RuleScore accumulatedScore, Parse<T> addedChild,
			TokenSequence<T> addedSeq, RuleScore addedScore) {

		// parses are always considered one piece
		return new RuleScore(accumulatedScore.sgt + addedScore.sgt);
	}

	public RuleScore combineHypotheses(TokenSequence<T> tokensFromHyp1, RuleScore scoreFromHyp1,
			TokenSequence<T> tokensFromHyp2, RuleScore scoreFromHyp2,
			TokenSequence<T> combinedTokenSequence) {

		// add fragments from each decoder hypothesis
		return new RuleScore(scoreFromHyp1.sgt + scoreFromHyp2.sgt);
	}

	public RuleScore combineRuleScoreWithChildren(Parse<T> parse, RuleScore parseScore,
			R ruleToAppend) {

		// parses are always considered one piece
		return new RuleScore(parseScore.sgt + ruleToAppend.getLogProb());
	}

	public RuleScore getInitialScore() {

		// start with one piece
		return new RuleScore(1.0);
	}

	public RuleScore scoreTerminalToken(T token) {
		return getInitialScore();
	}

	public RuleScore scoreTerminalToken(TokenSequence<T> singleToken) {
		return getInitialScore();
	}

	public String getFeatureName() {
		return "rule";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "sgt" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}
}
