package info.jonclark.treegraft.core.tokens.string;

import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.util.HashMap;

public class StringTokenFactory extends TokenFactory<StringToken> {

	private final HashMap<String, StringToken> str2tok = new HashMap<String, StringToken>();

	@Override
	public String getTokenAsString(StringToken tok) {
		if (tok.terminal) {
			return tok.str;
		} else {
			return tok.str.substring(NON_TERMINAL_PREFIX.length(), tok.str.length()
					- NON_TERMINAL_SUFFIX.length());
		}
	}

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
			str2tok.put(str, tok);
		}
		return tok;
	}

	public StringToken[] makeTerminalTokens(String[] strs) {
		StringToken[] toks = new StringToken[strs.length];
		for (int i = 0; i < toks.length; i++)
			toks[i] = makeToken(strs[i], true);
		return toks;
	}

	@Override
	public StringToken[] newTokenArray(int length) {
		return new StringToken[length];
	}
}