package info.jonclark.treegraft.core;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * A grammatical parse for a given input sequence.
 */
public class Parse<R extends GrammarRule<T>, T extends Token> {

	private Key<R, T> root;
	private String lispTree;

	/**
	 * Create a new <code>Parse</code> from its string representation.
	 * 
	 * @param lispTree
	 */
	public Parse(String lispTree) {
		this.lispTree = lispTree;
	}

	/**
	 * Sets the <code>Key</code> that is the root (top-most projection of this
	 * parse tree).
	 * 
	 * @param root
	 */
	public void setRoot(Key<R, T> root) {
		this.root = root;
	}

	/**
	 * Gets the <code>Key</code> that is the root (top-most projection of this
	 * parse tree).
	 * 
	 * @return a key
	 */
	public Key<R, T> getRoot() {
		return root;
	}

	/**
	 * Get the string representation of this parse.
	 */
	public String toString() {
		return lispTree;
	}
}
