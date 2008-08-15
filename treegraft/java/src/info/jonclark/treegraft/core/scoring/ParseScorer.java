package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public interface ParseScorer<R extends GrammarRule<T>, T extends Token> {
	
	public double combineRuleScoreWithChildren(double currentLogProb, R ruleToAppend);
	
	public double combineChildScores(double accumulatedLogProb, double newChildScore);
}
