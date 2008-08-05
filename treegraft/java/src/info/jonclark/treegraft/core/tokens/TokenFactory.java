package info.jonclark.treegraft.core.tokens;

/**
 * Responsible for mapping input strings to unique tokens so that they can be
 * efficiently compared and hashed. Also, the SymbolFactory must ensure that the
 * identifiers produced for terminals and nonterminals never overlap. (e.g. We
 * don't want the string literal "A" to collide with the non-terminal "A").
 * 
 * @author jon
 * @param <T>
 * @param <N>
 */
public abstract class TokenFactory<T extends Token> {
	
	public static final String NON_TERMINAL_PREFIX = "___";
	public static final String NON_TERMINAL_SUFFIX = "___";

	public abstract T makeToken(String str, boolean terminal);

	public abstract String getTokenAsString(T tok);
	
	public abstract T[] newTokenArray(int length);

	public String[] getTokensAsStrings(T[] toks) {
		String[] strs = new String[toks.length];
		for (int i = 0; i < toks.length; i++)
			strs[i] = getTokenAsString(toks[i]);
		return strs;
	}

	public abstract T[] makeTerminalTokens(String[] strs);
}
