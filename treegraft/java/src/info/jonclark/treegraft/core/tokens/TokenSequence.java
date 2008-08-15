package info.jonclark.treegraft.core.tokens;

public interface TokenSequence<T extends Token> {
	public T[] getTokens();
	
	public int hashCode();
	
	public boolean equals(Object obj);
}
