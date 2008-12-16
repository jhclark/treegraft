package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

public interface Scorer<R extends GrammarRule<T>, T extends Token> {

	/**
	 * Score a parse consisting of a single terminal token (and possibly a <s>
	 * or </s>). The parse's score element will be null.
	 * 
	 * @param token
	 * @param tokenIndex
	 * @param inputSentence
	 * @return
	 */
	public FeatureScores scoreTerminalParse(Parse<T> terminalParse);

	public FeatureScores combineRuleScoreWithChildren(Parse<T> currentLogProb, R ruleToAppend,
			List<T> inputSentence);

	public FeatureScores combineChildParseScores(Parse<T> accumulatedParse, Parse<T> addedChild,
			List<T> inputSentence);

	public FeatureScores recombineParses(Parse<T> a, Parse<T> b);

	public FeatureScores combineHypotheses(DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2,
			TokenSequence<T> combinedTokenSequence, List<T> inputSentence);

	public FeatureScores recombineHypotheses(DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2);

	public FeatureScores getInitialFeatureScores();

	public String[] getFeatureProbVectorLabels();

	public double[] getFeatureWeightVector();

	public void setFeatureWeightVector(double[] lambdas);
}
