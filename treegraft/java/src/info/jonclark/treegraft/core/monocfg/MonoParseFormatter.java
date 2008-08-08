package info.jonclark.treegraft.core.monocfg;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.scoring.BasicScorer;
import info.jonclark.treegraft.core.scoring.ParseScorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

/**
 * A ParseFormatter for a MonoCFGRule.
 */
public class MonoParseFormatter<T extends Token> extends ParseFormatter<MonoCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final ParseScorer<MonoCFGRule<T>,T> scorer = new BasicScorer<MonoCFGRule<T>, T>();

	public MonoParseFormatter(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	@Override
	public String formatNonterminalAfter(Key<MonoCFGRule<T>, T> key, double score) {
		return ")";
	}

	@Override
	public String formatNonterminalBefore(Key<MonoCFGRule<T>, T> key, double score) {
		return "(" + tokenFactory.getTokenAsString(key.getActiveArc().getRule().getLhs())
				+ " ";
	}

	@Override
	public String formatTerminal(T token) {
		return tokenFactory.getTokenAsString(token);
	}

	@Override
	public int[] getTargetToSourceRhsAlignment(Key<MonoCFGRule<T>, T> key) {
		return super.getMonotonicAlignment(key.getRule().getRhs().length);
	}

	@Override
	public T[] transduce(Key<MonoCFGRule<T>, T> key) {
		return key.getRule().getRhs();
	}

	@Override
	public ParseScorer<MonoCFGRule<T>, T> getScorer() {
		return scorer;
	}
}
