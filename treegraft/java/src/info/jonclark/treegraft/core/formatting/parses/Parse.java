package info.jonclark.treegraft.core.formatting.parses;

import info.jonclark.lang.DoubleEndedStringBuilder;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * A grammatical parse for a given input sequence.
 */
public class Parse<R extends GrammarRule<T>, T extends Token> {

	private Key<R, T> root;
	private DoubleEndedStringBuilder lispTree;
	private double currentScore;

	/**
	 * Create a new <code>Parse</code> from its string representation.
	 * 
	 * @param lispTree
	 */
	public Parse() {
		this.lispTree = new DoubleEndedStringBuilder();
	}
	
	/**
	 * Copy constructor for parse
	 * 
	 * @param other
	 */
	public Parse(Parse other) {
		this.root = other.root;
		this.lispTree = new DoubleEndedStringBuilder(other.lispTree);
		this.currentScore = other.currentScore;
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
	

	public double getLogProb() {
		return currentScore;
	}

	public void setCurrentScore(double currentScore) {
		this.currentScore = currentScore;
	}
	
	public void prepend(String str) {
		lispTree.prepend(str);
	}
	
	public void append(String str) {
		lispTree.append(str);
	}
	
	public void append(Parse<R,T> parse) {
		lispTree.append(parse.lispTree.toString());
	}

	/**
	 * Get the string representation of this parse.
	 */
	public String toString() {
		return lispTree.toString().trim();
	}
}
