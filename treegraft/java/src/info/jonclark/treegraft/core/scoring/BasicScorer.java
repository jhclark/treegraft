package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public class BasicScorer<R extends GrammarRule<T>, T extends Token> implements ParseScorer<R, T> {

	public double combineRuleScoreWithChildren(double currentLogProb, R ruleToAppend) {
		return currentLogProb + ruleToAppend.getLogProb();
	}

	public double combineChildScores(double accumulatedLogProb, double newChildScore) {
		return accumulatedLogProb + newChildScore;
	}

}
