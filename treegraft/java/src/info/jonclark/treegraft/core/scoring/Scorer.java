package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface Scorer<R extends GrammarRule<T>, T extends Token> {

	public FeatureScores scoreTerminalToken(T token);

	public FeatureScores combineRuleScoreWithChildren(Parse<T> currentLogProb, R ruleToAppend);

	public FeatureScores combineChildParseScores(Parse<T> accumulatedParse, Parse<T> addedChild);

	public FeatureScores recombineParses(Parse<T> a, Parse<T> b);

	public FeatureScores combineHypotheses(DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2,
			TokenSequence<T> combinedTokenSequence);

	public FeatureScores getInitialFeatureScores();

}
