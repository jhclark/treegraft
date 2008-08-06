package info.jonclark.treegraft.core.rules;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.formatting.parses.MonoParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.util.StringUtils;

import java.io.File;

/**
 * A monolingual version of a Context-Free GrammarRule.
 * 
 * @see MonoCFGRuleFactory
 * @see MonoParseFormatter
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class MonoCFGRule<T extends Token> implements GrammarRule<T> {

	private final T lhs;
	private final T[] rhs;
	private final Constraint[] constraints;

	private final String id;
	private final File file;
	private final int lineNumber;

	private final SecondTimer cost = new SecondTimer(true, false);

	/**
	 * Creates a new <code>MonoCFGRule</code>.
	 * 
	 * @param lhs
	 *            The source-side left hand side of this rule as a non-terminal
	 *            token. e.g. The "S" in "S -> NP VP"
	 * @param rhs
	 *            the source-side right hand side of this rule in which
	 *            terminals and non-terminals can be freely mixed. e.g. The
	 *            "NP freely VP" in "S -> NP freely VP"
	 * @param constraints
	 *            the unification constraints associated with this GrammarRule.
	 * @param id
	 *            the user-specified identifier for this rule
	 * @param file
	 *            the File in which this GrammarRule was defined
	 * @param lineNumber
	 *            the line number of the grammar file on which this GrammarRule
	 *            was defined
	 */
	public MonoCFGRule(T lhs, T[] rhs, Constraint[] constraints, String id, File file,
			int lineNumber) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.file = file;
		this.id = id;
		this.lineNumber = lineNumber;
		this.constraints = constraints;
	}

	/**
	 * {@inheritDoc}
	 */
	public T getLhs() {
		return lhs;
	}

	/**
	 * {@inheritDoc}
	 */
	public T[] getRhs() {
		return rhs;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLength() {
		return rhs.length;
	}

	/**
	 * {@inheritDoc}
	 */
	public void beginEvaluation() {
		cost.go();
	}

	/**
	 * {@inheritDoc}
	 */
	public void stopEvaluation() {
		cost.pause();
	}

	/**
	 * {@inheritDoc}
	 */
	public String getTimeCost() {
		return cost.getSecondsFormatted();
	}

	/**
	 * {@inheritDoc}
	 */
	public Constraint[] getConstraints() {
		return constraints;
	}

	/**
	 * {@inheritDoc}
	 */
	public File getFile() {
		return file;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getLogProb() {
		return 1.0;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getRuleId() {
		return id;
	}

	/**
	 * Gets a string representation of this <code>MonoCFGRule</code>.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		return lhs.getId() + " -> " + StringUtils.untokenize(rhs);
	}
}
