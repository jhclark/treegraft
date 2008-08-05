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
 * @author jon
 * @param <T>
 */
public interface GrammarRule<T extends Token> {

	/**
	 * Get the source-side left hand side of this rule as a non-terminal token.
	 * e.g. The "S" in "S -> NP VP"
	 * 
	 * @return
	 */
	public T getLhs();

	/**
	 * Get the source-side right hand side of this rule in which terminals and
	 * non-terminals can be freely mixed. e.g. The "NP freely VP" in "S -> NP
	 * freely VP"
	 * 
	 * @return
	 */
	public T[] getRhs();

	/**
	 * Get the length of the source RHS of this rule in terms of terminal and
	 * non-terminal rules.
	 * 
	 * @return
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
	 * @return
	 */
	public String getTimeCost();

	/**
	 * Get the unification constraints assocaited with this GrammarRule.
	 * 
	 * @return
	 */
	public Constraint[] getConstraints();

	/**
	 * Get the score of this rule, in the log probability domain.
	 * 
	 * @return
	 */
	public double getLogProb();

	/**
	 * Get the File in which this GrammarRule was defined.
	 * 
	 * @return
	 */
	public File getFile();

	/**
	 * Get the line number of the grammar file on which this GrammarRule was
	 * defined.
	 * 
	 * @return
	 */
	public int getLineNumber();
	
	/**
	 * Gets the user-specified identifier for this rule
	 * 
	 * @return
	 */
	public String getRuleId();
}
