package info.jonclark.treegraft.core.tokens.string;

import info.jonclark.treegraft.core.tokens.Token;

/**
 * Represents tokens as unique Strings so that comparisons can be done quickly,
 * while still retaining ease of debugging.
 * 
 * @author Jonathan Clark
 */
public class StringToken implements Token {

	protected final String str;
	protected final boolean terminal;

	protected StringToken(String str, boolean terminal) {
		this.str = str;
		this.terminal = terminal;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return str;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		if (obj instanceof StringToken) {
			StringToken tok = (StringToken) obj;

			return (this == tok);
		} else if (obj instanceof String) {
			String other = (String) obj;

			return other.equals(this.str);
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return str.hashCode();
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
	public int compareTo(Token o) {
		return this.hashCode() - o.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return str;
	}

	public String getWord() {
		return str;
	}
}
