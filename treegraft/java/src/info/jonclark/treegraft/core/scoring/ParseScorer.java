package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public interface ParseScorer<R extends GrammarRule<T>, T extends Token> {
	public double accumulate(double currentLogProb, R ruleToAppend);
}
