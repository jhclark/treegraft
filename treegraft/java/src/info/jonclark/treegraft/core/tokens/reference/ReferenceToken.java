package info.jonclark.treegraft.core.tokens.reference;

import info.jonclark.treegraft.core.tokens.Token;

/**
 * Represents tokens as integers so that comparisons can be done quickly.
 * 
 * @author Jonathan Clark
 */
public class ReferenceToken implements Token {

	protected ReferenceToken() {
	}

	/**
	 * {@inheritDoc}
	 */
	public String getId() {
		return super.hashCode() + "";
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj) {
		return (this == obj);
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isTerminal() {
		
		if(this instanceof TerminalReferenceToken) {
			return true;
		} else if(this instanceof NonterminalReferenceToken) {
			return false;
		} else {
			throw new RuntimeException("Unknown Reference Token Type: " + this);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return getId();
	}

}
