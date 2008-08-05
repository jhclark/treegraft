package info.jonclark.treegraft.core.formatting;

public interface InputSentenceFormatter {

	/**
	 * Returns true if the input source has any more sentences to be translated.
	 * 
	 * @return
	 */
	public boolean hasNext();

	/**
	 * Returns the next sentence to be translated in which tokens are space
	 * delimited.
	 * 
	 * @return
	 */
	public String next();
}
