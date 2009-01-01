package info.jonclark.treegraft.core.tokens;

import java.util.List;

/**
 * Responsible for mapping input strings to unique tokens so that they can be
 * efficiently compared and hashed. Also, the SymbolFactory must ensure that the
 * identifiers produced for terminals and nonterminals never overlap. (e.g. We
 * don't want the string literal "A" to collide with the non-terminal "A").
 * 
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public abstract class TokenFactory<T extends Token> {

	/**
	 * A prefix for non-terminals to distinguish them from terminals when using
	 * StringTokens
	 */
	public static final String NON_TERMINAL_PREFIX = "___";

	/**
	 * A suffix for non-terminals to distinguish them from terminals when using
	 * StringTokens
	 */
	public static final String NON_TERMINAL_SUFFIX = "___";

	/**
	 * Creates a new unique token instance for each unique string given to it
	 * (though terminal and non-terminal tokens having the same string should
	 * also have unique <code>Token</code> instances).
	 * 
	 * @param str
	 *            the string to be turned into a <code>Token</code>
	 * @param terminal
	 * @return
	 */
	public abstract T makeToken(String str, boolean terminal);

	/**
	 * Creates a new <code>TokenSequence</code> given an array of tokens. A
	 * <TokenSequence> can allow for faster aggregate token comparisons for some
	 * token types.
	 * 
	 * @param tokens
	 *            the tokens to be included in the sequence
	 * @return a token sequence of the tokens in the given array
	 */
	public abstract TokenSequence<T> makeTokenSequence(List<T> tokens);

	/**
	 * Creates a new <code>Token</code> array of the current type to get around
	 * the defficiency in Java's generics of not being able to create generic
	 * arrays.
	 * 
	 * @param length
	 *            the desired length of the array to be returned
	 * @return an empty <code>Token</code> array
	 */
	public abstract T[] newTokenArray(int length);

	/**
	 * A convenience method that turns an input sequence from a String array
	 * into a Token array.
	 * 
	 * @param strs
	 *            the input sequence to be token-ified
	 * @param terminals
	 *            Should the created tokens be terminal tokens?
	 * @return a token array of terminals that represent the given strings
	 */
	public abstract T[] makeTokens(String[] strs, boolean terminals);

	public String[] getTokensAsStrings(T[] tokens) {
		String[] arr = new String[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			arr[i] = tokens[i].getWord();
		}
		return arr;
	}

	public String[] getTokensAsStrings(List<T> tokens) {
		String[] arr = new String[tokens.size()];
		for (int i = 0; i < tokens.size(); i++) {
			arr[i] = tokens.get(i).getWord();
		}
		return arr;
	}
}
