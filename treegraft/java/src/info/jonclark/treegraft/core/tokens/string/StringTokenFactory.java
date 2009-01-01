package info.jonclark.treegraft.core.tokens.string;

import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.HashMap;
import java.util.List;

/**
 * A <code>TokenFactory</code> implementation for <code>StringTokens</code>.
 * 
 * @author Jonathan Clark
 */
public class StringTokenFactory extends TokenFactory<StringToken> {

	private final HashMap<String, StringToken> str2tok = new HashMap<String, StringToken>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StringToken makeToken(String str, boolean terminal) {
		String key;
		if (terminal) {
			key = str;
		} else {
			key = NON_TERMINAL_PREFIX + str + NON_TERMINAL_SUFFIX;
		}

		StringToken tok = str2tok.get(key);
		if (tok == null) {
			tok = new StringToken(key, terminal);
			str2tok.put(key, tok);
		}
		return tok;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TokenSequence<StringToken> makeTokenSequence(List<StringToken> tokens) {
		return new StringTokenSequence(tokens);
	}

	/**
	 * {@inheritDoc}
	 */
	public StringToken[] makeTokens(String[] strs, boolean terminals) {
		StringToken[] toks = new StringToken[strs.length];
		for (int i = 0; i < toks.length; i++)
			toks[i] = makeToken(strs[i], terminals);
		return toks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public StringToken[] newTokenArray(int length) {
		return new StringToken[length];
	}
}
