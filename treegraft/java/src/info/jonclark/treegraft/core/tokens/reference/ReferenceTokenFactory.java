package info.jonclark.treegraft.core.tokens.reference;

import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.HashMap;
import java.util.List;

/**
 * A <code>TokenFactory</code> implementation for <code>IntegerTokens</code>.
 * 
 * @author Jonathan Clark
 */
public class ReferenceTokenFactory extends TokenFactory<ReferenceToken> {

	private static final int DEFAULT_VOCAB_SIZE = 10000;
	private final HashMap<String, ReferenceToken> str2tok = new HashMap<String, ReferenceToken>();
	private final HashMap<ReferenceToken, String> tok2str =
			new HashMap<ReferenceToken, String>(DEFAULT_VOCAB_SIZE);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTokenAsString(ReferenceToken tok) {
		return tok2str.get(tok);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReferenceToken makeToken(String str, boolean terminal) {

		String key;
		if (terminal) {
			key = str;
		} else {
			key = NON_TERMINAL_PREFIX + str + NON_TERMINAL_SUFFIX;
		}

		ReferenceToken tok = str2tok.get(key);
		if (tok == null) {
			int id = tok2str.size();
			if (terminal) {
				tok = new TerminalReferenceToken();
			} else {
				tok = new NonterminalReferenceToken();
			}
			str2tok.put(str, tok);
			tok2str.put(tok, str);
		}
		return tok;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TokenSequence<ReferenceToken> makeTokenSequence(List<ReferenceToken> tokens) {
		return new ReferenceTokenSequence(tokens);
	}

	/**
	 * {@inheritDoc}
	 */
	public ReferenceToken[] makeTokens(String[] strs, boolean terminals) {
		ReferenceToken[] toks = new ReferenceToken[strs.length];
		for (int i = 0; i < toks.length; i++)
			toks[i] = makeToken(strs[i], terminals);
		return toks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ReferenceToken[] newTokenArray(int length) {
		return new ReferenceToken[length];
	}
}
