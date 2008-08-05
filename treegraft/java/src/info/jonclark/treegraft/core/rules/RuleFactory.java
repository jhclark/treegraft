package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;

public interface RuleFactory<R extends GrammarRule<T>, T extends Token> {
	public R makeDummyRule(T token);
	public ParseFormatter<R,T> getDefaultFormatter();
	public ParseFormatter<R,T>[] getDebugFormatters();
}
