package info.jonclark.treegraft.core.tokens;

import info.jonclark.lang.trie.Gettable;

import java.util.List;

/**
 * A sequence of one or more tokens (zero length is not allowed).
 * 
 * @author jon
 *
 * @param <T>
 */
public interface TokenSequence<T extends Token> extends Gettable<T> {

	public T get(int i);
	
	public List<T> getTokens();

	public TokenSequence<T> keepNLeftMostTokens(int n);

	public TokenSequence<T> keepNRightMostTokens(int n);

	public TokenSequence<T> subsequence(int nStart, int nEnd);

	public TokenSequence<T> append(TokenSequence<T> suffix);

	public int size();

	public int hashCode();

	public boolean equals(Object obj);
}
