package info.jonclark.treegraft.core.formatting;

import info.jonclark.treegraft.core.tokens.Token;

/**
 * Picks the sentences from raw documents that should be parsed.
 * 
 * @author Jonathan Clark
 */
public interface InputSentenceFormatter<T extends Token> {

	/**
	 * Returns true if the input source has any more sentences to be parsed.
	 * 
	 * @return True if there are more sentences remaining; false otherwise
	 */
	public boolean hasNext();

	/**
	 * Returns the next sentence to be parsed as an array of tokens.
	 * 
	 * @return the next sentence to be parsed
	 */
	public T[] next();
}
