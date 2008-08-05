package info.jonclark.treegraft.core.rules;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.treegraft.util.ProbUtils;
import info.jonclark.util.StringUtils;

import java.io.File;

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

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getLhs()
	 */
	public T getLhs() {
		return lhs;
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getRhs()
	 */
	public T[] getRhs() {
		return rhs;
	}

	public T getTargetLhs() {
		return targetLhs;
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getRhs()
	 */
	public T[] getTargetRhs() {
		return targetRhs;
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getLength()
	 */
	public int getLength() {
		return rhs.length;
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#beginEvaluation()
	 */
	public void beginEvaluation() {
		cost.go();
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#stopEvaluation()
	 */
	public void stopEvaluation() {
		cost.pause();
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getTimeCost()
	 */
	public String getTimeCost() {
		return cost.getSecondsFormatted();
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.parser.core.GRule#getConstraints()
	 */
	public Constraint[] getConstraints() {
		return constraints;
	}

	/**
	 * Gets the user-specified rule identifier.
	 */
	public String getRuleId() {
		return ruleId;
	}

	public String toString() {
		return ruleId + "@" + file.getName() + ":" + lineNumber + " = " + lhs + "::" + targetLhs
				+ " : [ " + StringUtils.untokenize(rhs) + " ] -> [ "
				+ StringUtils.untokenize(targetRhs) + " ]";
	}

	public File getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Returns the rule score in the log probability domain.
	 */
	public double getLogProb() {
		return logProb;
	}

	/**
	 * Returns the alignment as an int array having the same length as the
	 * source RHS, where each index corresponds to a source word and each value
	 * corresponds to a target word. An array index containing the value -1
	 * indicates that the source word is unaligned.
	 * 
	 * @return
	 */
	public int[] getAlignment() {
		return alignment;
	}
}
