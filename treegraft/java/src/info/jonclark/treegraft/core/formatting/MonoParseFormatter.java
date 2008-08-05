package info.jonclark.treegraft.core.formatting;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.MonoCFGRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

public class MonoParseFormatter<T extends Token> extends ParseFormatter<MonoCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;

	public MonoParseFormatter(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	@Override
	public String formatNonterminalAfter(Key<MonoCFGRule<T>, T> key) {
		return ")";
	}

	@Override
	public String formatNonterminalBefore(Key<MonoCFGRule<T>, T> key) {
		return "(" + tokenFactory.getTokenAsString(key.getActiveArc().getRule().getLhs())
				+ " ";
	}

	@Override
	public String formatTerminal(T token) {
		return tokenFactory.getTokenAsString(token);
	}

	@Override
	public int[] getRhsAlignment(Key<MonoCFGRule<T>, T> key) {
		return super.getMonotonicAlignment(key.getRule().getRhs().length);
	}

	@Override
	public T[] transduce(Key<MonoCFGRule<T>, T> key) {
		return key.getRule().getRhs();
	}
}
