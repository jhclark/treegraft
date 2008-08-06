package info.jonclark.treegraft.core.formatting.forest;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * Transforms a <code>Chart</code> into a parse forest of some arbitrary data
 * type.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 * @param <F>
 *            Target return data type for the ParseForest
 */
public abstract class ParseForestFormatter<R extends GrammarRule<T>, T extends Token, F> {

	/**
	 * Adds a non-terminal <code>Key</code> to the parse forest being built
	 * internally by this <code>ParseForestFormatter</code>.
	 * 
	 * @param key
	 *            the key to be added
	 */
	public abstract void addNonterminal(Key<R, T> key);

	/**
	 * Adds a terminal <code>Key</code> to the parse forest being built
	 * internally by this <code>ParseForestFormatter</code>.
	 * 
	 * @param key
	 *            the key to be added
	 */
	public abstract void addTerminal(Key<R, T> key);

	/**
	 * Gets the finished parse forest of type F that was created by this
	 * <code>ParseForestFormatter</code>.
	 * 
	 * @return a complete parse forest
	 */
	public abstract F getParseForest();
}
