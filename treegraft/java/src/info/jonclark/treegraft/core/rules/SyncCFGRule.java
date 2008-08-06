package info.jonclark.treegraft.core.rules;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.formatting.parses.SyncParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.treegraft.util.ProbUtils;
import info.jonclark.util.StringUtils;

import java.io.File;

/**
 * A synchronous version of a Context-Free GrammarRule.
 * 
 * @see SyncCFGRuleFactory
 * @see SyncParseFormatter
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class SyncCFGRule<T extends Token> implements GrammarRule<T> {

	private final T lhs;
	private final T[] rhs;
	private final T targetLhs;
	private final T[] targetRhs;

	private final int[] alignment; // zero-based alignments; negative value
	// indicates no alignment
	private final double logProb;

	private final String ruleId;

	private final File file;
	private final int lineNumber;

	private final Constraint[] constraints;
	private final SecondTimer cost = new SecondTimer(true, false);

	/**
	 * Creates a new <code>SyncCFGRule</code>.
	 * 
	 * @param lhs
	 * @param rhs
	 * @param targetLhs
	 * @param targetRhs
	 * @param ruleName
	 * @param ruleId
	 * @param alignment
	 * @param prob
	 *            A score in the probability domain (internally, it will be
	 *            converted to log prob)
	 * @param constraints
	 * @param file
	 * @param lineNumber
	 */
	public SyncCFGRule(T lhs, T[] rhs, T targetLhs, T[] targetRhs, String ruleId, int[] alignment,
			double prob, Constraint[] constraints, File file, int lineNumber) {

		this.lhs = lhs;
		this.rhs = rhs;
		this.targetLhs = targetLhs;
		this.targetRhs = targetRhs;
		this.ruleId = ruleId;
		this.alignment = alignment;
		this.logProb = ProbUtils.logProb(prob);
		this.constraints = constraints;

		this.file = file;
		this.lineNumber = lineNumber;
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
	 * Gets the target-side LHS of this rule
	 * 
	 * @return the constituent for the target LHS of this rule
	 */
	public T getTargetLhs() {
		return targetLhs;
	}

	/**
	 * Get the target-side right hand side of this rule in which terminals and
	 * non-terminals can be freely mixed. e.g. The "NP freely VP" in "S -> NP
	 * freely VP"
	 * 
	 * @return the constituents that form the target RHS of this rule
	 */
	public T[] getTargetRhs() {
		return targetRhs;
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
	public String getRuleId() {
		return ruleId;
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
		return logProb;
	}

	/**
	 * Returns the alignment as an int array having the same length as the
	 * source RHS, where each index corresponds to a source constituent and each
	 * value corresponds to a target constituent. An array index containing the
	 * value -1 indicates that the source constituent is unaligned.
	 * 
	 * @return
	 */
	public int[] getAlignment() {
		return alignment;
	}

	/**
	 * Gets a string representation of this <code>SyncCFGRule</code>.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		return ruleId + "@" + file.getName() + ":" + lineNumber + " = " + lhs + "::" + targetLhs
				+ " : [ " + StringUtils.untokenize(rhs) + " ] -> [ "
				+ StringUtils.untokenize(targetRhs) + " ]";
	}
}
