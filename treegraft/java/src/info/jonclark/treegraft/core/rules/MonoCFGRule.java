package info.jonclark.treegraft.core.rules;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.util.StringUtils;

import java.io.File;

public class MonoCFGRule<T extends Token> implements GrammarRule<T> {

	private final T lhs;
	private final T[] rhs;
	private final Constraint[] constraints;

	private final String id;
	private final File file;
	private final int lineNumber;

	private final SecondTimer cost = new SecondTimer(true, false);

	public MonoCFGRule(T lhs, T[] rhs, Constraint[] constraints, String id, File file, int lineNumber) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.file = file;
		this.id = id;
		this.lineNumber = lineNumber;
		this.constraints = constraints;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#getLhs()
	 */
	public T getLhs() {
		return lhs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#getRhs()
	 */
	public T[] getRhs() {
		return rhs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#getLength()
	 */
	public int getLength() {
		return rhs.length;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#beginEvaluation()
	 */
	public void beginEvaluation() {
		cost.go();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#stopEvaluation()
	 */
	public void stopEvaluation() {
		cost.pause();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#getTimeCost()
	 */
	public String getTimeCost() {
		return cost.getSecondsFormatted();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see info.jonclark.parser.core.GRule#getConstraints()
	 */
	public Constraint[] getConstraints() {
		return constraints;
	}

	public File getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public double getLogProb() {
		return 1.0;
	}

	public String toString() {
		return lhs.getId() + " -> " + StringUtils.untokenize(rhs);
	}

	public String getRuleId() {
		return id;
	}
}
