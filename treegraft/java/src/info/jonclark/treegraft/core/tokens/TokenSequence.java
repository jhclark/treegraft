package info.jonclark.treegraft.core.tokens;

import java.util.List;

public interface TokenSequence<T extends Token> {
	
	public List<T> getTokens();
	
	public TokenSequence<T> keepNLeftMostTokens(int n);
	
	public TokenSequence<T> keepNRightMostTokens(int n);
	
	public TokenSequence<T> prepend(TokenSequence<T> seq);
	
	public int length();
	
	public int hashCode();
	
	public boolean equals(Object obj);
}
