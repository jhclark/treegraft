package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.synccfg.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;

/**
 * A basic grammar rule for a monolingual or transduction parser. Other data can
 * easily be placed inside both implementations of GrammarRule (as in
 * synchronous transduction information) and in the Token implementations (e.g.
 * tokens for use in factored translation). Also, it is important for
 * <code>GrammarRule</code> implementations to implement
 * <code>equals(Object)</code> and <code>hashCode()</code> as these will be
 * called many times for hashing by the <code>ActiveArcManager</code>.
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
	 * @return a time in seconds as a double
	 */
	public double getTimeCost();

	/**
	 * Get the unification constraints associated with this GrammarRule.
	 * 
	 * @return an array of constraints
	 */
	// public Constraint[] getConstraints();
	/**
	 * Get the score of this rule, in the log probability domain.
	 * 
	 * @return a log probability
	 */
	public double getLogProb();

	/**
	 * Get the File in which this GrammarRule was defined.
	 * 
	 * @return a file, which can be converted into string representations for
	 *         debugging
	 */
	// public File getFile();
	/**
	 * Get the line number of the grammar file on which this GrammarRule was
	 * defined.
	 * 
	 * @return a line number as an integer
	 */
	// public int getLineNumber();
	/**
	 * Gets the user-specified identifier for this rule
	 * 
	 * @return an identifier string such as NP,1001
	 */
	public String getRuleId();

	/**
	 * Checks to make sure the proposed key can legally extend an
	 * <code>ActiveArc</code> according to this rule.
	 * 
	 * @param <R>
	 *            the <code>GrammarRule</code> type. This will likely need to be
	 *            casted to the implementing type of GrammarRule. Though this
	 *            could theoretically be made type safe, it would require
	 *            passing around an unacceptable number of type paramaters and
	 *            doing so in a way not technically supported by Java).
	 * @param sourceRhsIndex
	 *            the source-side RHS rule index that is about to be extended in
	 *            an <code>ActiveArc</code>
	 * @param childRule
	 *            a proposed child rule whose LHS will be applied to the RHS of
	 *            this parent <code>GrammarRule</code>
	 * @return true if the proposed key is legal according to this rule; false
	 *         otherwise.
	 * @see SyncCFGRule
	 */
	public <R extends GrammarRule<T>> boolean areConstraintsSatisfied(int sourceRhsIndex,
			R childRule);

	/**
	 * Assuming that an existing arc and a candidate arc have the same start and
	 * end points in the input sequence and both arcs have the same constituent
	 * to the right of their dots, then this methodgets any further items that
	 * must be matched by the candidate arc for that arc to be packed into an
	 * existing arc. (Usually the source-side LHS concatenated with the
	 * source-side RHS).
	 * 
	 * @return the items that must be matched as a sequence of tokens
	 */
	public TokenSequence<T> getArcPackingString();

	public T getKeyPackingString();
	
	public void incrementKeysCreated();
	
	public int getKeysCreated();
}
