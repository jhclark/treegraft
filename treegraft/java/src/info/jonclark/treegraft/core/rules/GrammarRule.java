package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.unification.Constraint;

import java.io.File;

/**
 * A basic grammar rule for a monolingual or transduction parser. Other data can
 * easily be placed inside both implementations of GrammarRule (as in
 * synchronous transduction information) and in the Token implementations (e.g.
 * tokens for use in factored translation).
 * 
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public interface GrammarRule<T extends Token> {

	/**
	 * Get the source-side left hand side of this rule as a non-terminal token.
	 * e.g. The "S" in "S -> NP VP"
	 * 
	 * @return the constituent for the LHS of this rule
	 */
	public T getLhs();

	/**
	 * Get the source-side right hand side of this rule in which terminals and
	 * non-terminals can be freely mixed. e.g. The "NP freely VP" in "S -> NP
	 * freely VP"
	 * 
	 * @return the constituents that form the RHS of this rule
	 */
	public T[] getRhs();

	/**
	 * Get the length of the source RHS of this rule in terms of terminal and
	 * non-terminal rules.
	 * 
	 * @return the length of the RHS of this rule
	 */
	public int getLength();

	/**
	 * Begin timing the evaluation of this rule for profiling purposes.
	 */
	public void beginEvaluation();

	/**
	 * Stop timing the evaluation of this rule for profiling purposes.
	 */
	public void stopEvaluation();

	/**
	 * Get the number of seconds consumed by evaluating this rule during the
	 * lifetime of this GrammarRule.
	 * 
	 * @return a time in seconds as a string
	 */
	public String getTimeCost();

	/**
	 * Get the unification constraints associated with this GrammarRule.
	 * 
	 * @return an array of constraints
	 */
	public Constraint[] getConstraints();

	/**
	 * Get the score of this rule, in the log probability domain.
	 * 
	 * @return a log probability
	 */
	public double getLogProb();

	/**
	 * Get the File in which this GrammarRule was defined.
	 * 
	 * @return a file, which can be converted into string representations for debugging
	 */
	public File getFile();

	/**
	 * Get the line number of the grammar file on which this GrammarRule was
	 * defined.
	 * 
	 * @return a line number as an integer
	 */
	public int getLineNumber();

	/**
	 * Gets the user-specified identifier for this rule
	 * 
	 * @return an identifier string such as NP,1001
	 */
	public String getRuleId();
}
