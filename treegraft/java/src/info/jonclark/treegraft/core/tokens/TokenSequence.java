package info.jonclark.treegraft.core.tokens;

import java.util.List;

/**
 * A sequence of one or more tokens (zero length is not allowed). NOTE: <s> and
 * </s> sentence markers may be used to construct TokenSequences, but will NOT
 * be returned by getContentTokens().
 * 
 * @author jon
 * @param <T>
 */
public interface TokenSequence<T extends Token> {

	/**
	 * Includes BOS and EOS markers
	 * 
	 * @return
	 */
	public List<T> getContentTokens(TokenFactory<T> tokenFactory);

	public TokenSequence<T> keepNLeftMostTokens(int n);

	public TokenSequence<T> keepNRightMostTokens(int n);

	public TokenSequence<T> subsequence(int nStart, int nEnd);

	public TokenSequence<T> append(TokenSequence<T> suffix);

	/**
	 * INCLUDES BOS AND EOS MARKERS
	 */
	public int size();

	public int hashCode();

	public boolean equals(Object obj);
}
