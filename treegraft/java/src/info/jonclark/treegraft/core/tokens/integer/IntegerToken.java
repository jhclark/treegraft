package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.Token;

/**
 * Represents tokens as integers so that comparisons can be done quickly.
 * 
 * @author Jonathan Clark
 */
public class IntegerToken implements Token {

	protected final String word;
	protected final int id;
	protected final boolean terminal;

	protected IntegerToken(String word, int id, boolean terminal) {
		this.word = word;
		this.id = id;
		this.terminal = terminal;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return id + "";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		if (obj instanceof IntegerToken) {
			IntegerToken t = (IntegerToken) obj;
			return (this.id == t.id);
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTerminal() {
		return terminal;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getWord();
	}

	public int compareTo(Token other) {
		if (other instanceof IntegerToken) {
			return this.id - ((IntegerToken) other).id;
		} else {
			return -1;
		}
	}

	public String getWord() {
		return word;
	}
}
