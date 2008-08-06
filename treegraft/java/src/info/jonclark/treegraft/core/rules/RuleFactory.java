package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * A means for handling on-the-fly creation of dummy rules for terminal symbols
 * during the parsing process and for defining how partial parse output should
 * be formatted for use in debugging.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public interface RuleFactory<R extends GrammarRule<T>, T extends Token> {

	/**
	 * Creates a new "dummy" rule that will serve as a placeholder for a source
	 * terminal created on-the-fly for bookkeeping purposes.
	 * 
	 * @param token
	 *            The token for which the dummy rule is being created.
	 * @return the new dummy rule
	 */
	public R makeDummyRule(T token);

	/**
	 * Gets a <code>ParseFormatters</code> that provides a default method of
	 * visualizing this rule type.
	 * 
	 * @return an <code>ParseFormatter</code>
	 */
	public ParseFormatter<R, T> getDefaultFormatter();

	/**
	 * Gets an array of <code>ParseFormatters</code> that can provide several
	 * different ways of visualizing partial parses during debugging when using
	 * this rule type.
	 * 
	 * @return an array of <code>ParseFormatters</code>
	 */
	public ParseFormatter<R, T>[] getDebugFormatters();
}
