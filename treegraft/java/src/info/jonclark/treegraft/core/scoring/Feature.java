package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

// TODO: Divide this into Feature and MultiFeature where MultiFeature can return a feature vector
// TODO: How do we deal with parsing features from rules? Just require the rule type to deal with it?
public interface Feature<R extends GrammarRule<T>, T extends Token, S extends FeatureScore> {

	public S combineRuleScoreWithChildren(Parse<T> parse, S parseScore, R ruleToAppend);

	public S getInitialScore();

	public S combineChildParseScores(Parse<T> accumulatedParse, TokenSequence<T> accumulatedSeq,
			S accumulatedScore, Parse<T> addedChild, TokenSequence<T> addedSeq, S addedScore);

	public S scoreTerminalToken(TokenSequence<T> singleToken);

	public S combineHypotheses(TokenSequence<T> tokensFromHyp1, S scoreFromHyp1,
			TokenSequence<T> tokensFromHyp2, S scoreFromHyp2, TokenSequence<T> combinedTokenSequence);

	public String getFeatureName();

	/**
	 * Gets the mnemonic label names used for these (used for reading in lambda
	 * weights from properties file)
	 * 
	 * @return
	 */
	public String[] getFeatureProbVectorLabels();

	public double[] getFeatureWeightVector();
}
