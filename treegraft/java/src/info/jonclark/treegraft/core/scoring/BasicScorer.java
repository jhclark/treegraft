package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public class BasicScorer<R extends GrammarRule<T>, T extends Token> implements ParseScorer<R, T> {

	@Override
	public double accumulate(double currentLogProb, R ruleToAppend) {
		return currentLogProb + ruleToAppend.getLogProb();
	}

}
