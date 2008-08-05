package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.util.ArrayList;
import java.util.HashMap;

public class IntegerTokenFactory extends TokenFactory<IntegerToken> {

	private static final int DEFAULT_VOCAB_SIZE = 10000;
	private final HashMap<String, IntegerToken> str2tok = new HashMap<String, IntegerToken>();
	private final ArrayList<String> int2str = new ArrayList<String>(DEFAULT_VOCAB_SIZE);

	@Override
	public String getTokenAsString(IntegerToken tok) {
		return int2str.get(tok.id);
	}

	public IntegerToken makeToken(String str, boolean terminal) {

		String key;
		if (terminal) {
			key = str;
		} else {
			key = NON_TERMINAL_PREFIX + str + NON_TERMINAL_SUFFIX;
		}

		IntegerToken tok = str2tok.get(key);
		if (tok == null) {
			int id = int2str.size();
			tok = new IntegerToken(id, terminal);
			str2tok.put(str, tok);
			int2str.add(str);
		}
		return tok;
	}
	
	public IntegerToken[] makeTerminalTokens(String[] strs) {
		IntegerToken[] toks = new IntegerToken[strs.length];
		for (int i = 0; i < toks.length; i++)
			toks[i] = makeToken(strs[i], true);
		return toks;
	}

	@Override
	public IntegerToken[] newTokenArray(int length) {
		return new IntegerToken[length];
	}

}
