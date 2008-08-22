package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public class FragmentationPenalty<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, FragmentationScore> {

	private final double[] weights;

	public FragmentationPenalty(double weight) {
		this.weights = new double[] { weight };
	}

	public FragmentationScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, FragmentationScore accumulatedScore,
			Parse<T> addedChild, TokenSequence<T> addedSeq, FragmentationScore addedScore) {

		// parses are always considered one piece
		return accumulatedScore;
	}

	public FragmentationScore combineHypotheses(TokenSequence<T> tokensFromHyp1,
			FragmentationScore scoreFromHyp1, TokenSequence<T> tokensFromHyp2,
			FragmentationScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence) {

		// add fragments from each decoder hypothesis
		return new FragmentationScore(scoreFromHyp1.fragments + scoreFromHyp2.fragments);
	}

	public FragmentationScore combineRuleScoreWithChildren(Parse<T> parse,
			FragmentationScore parseScore, R ruleToAppend) {

		// parses are always considered one piece
		return parseScore;
	}

	public FragmentationScore getInitialScore() {

		// start with one piece
		return new FragmentationScore(1);
	}

	public FragmentationScore scoreParse(Parse<T> parse, TokenSequence<T> tokenSequence) {
		return getInitialScore();
	}

	public FragmentationScore scoreTerminalToken(T token) {
		return getInitialScore();
	}

	public FragmentationScore scoreTerminalToken(TokenSequence<T> singleToken) {
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
}
