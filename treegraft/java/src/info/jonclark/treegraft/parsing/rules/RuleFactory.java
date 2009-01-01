package info.jonclark.treegraft.parsing.rules;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.transduction.Transducer;

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

	public R makeGlueRule(T label, T[] sourceRhs, T[] targetRhs);

	/**
	 * Allows the specification of an alignment as an int array having the same
	 * length as the source RHS, where each index corresponds to a source
	 * constituent and each value corresponds to a target constituent.
	 */
	public R makeGlueRule(T label, T[] sourceRhs, T[] targetRhs, int[] sourceToTargetAlignment);

	public Transducer<R, T> getTransducer();
}
