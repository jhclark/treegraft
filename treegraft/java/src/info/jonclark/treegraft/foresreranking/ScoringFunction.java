package info.jonclark.treegraft.foresreranking;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public interface ScoringFunction<R extends GrammarRule<T>, T extends Token> {
	public double score(Key<R, T> key);
}
