package info.jonclark.treegraft.core.monocfg;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
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

	private final TokenSequence<T> packingString;

	private final String id;
	private final File file;
	private final int lineNumber;

	private int keysCreated = 0;

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
			int lineNumber, TokenFactory<T> tokenFactory) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.file = file;
		this.id = id;
		this.lineNumber = lineNumber;
		this.constraints = constraints;
		this.packingString = makePackingString(lhs, rhs, tokenFactory);
	}

	/**
	 * Creates a packing string for CFG rules
	 */
	public static <T extends Token> TokenSequence<T> makePackingString(T lhs, T[] rhs,
			TokenFactory<T> tokenFactory) {

		// create the packing string
		T[] packingArr = tokenFactory.newTokenArray(rhs.length + 1);
		packingArr[0] = lhs;
		System.arraycopy(rhs, 0, packingArr, 1, rhs.length);
		return tokenFactory.makeTokenSequence(packingArr);
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
	public double getTimeCost() {
		return cost.getSeconds();
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
	public File getFileAndLine() {
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
	 * {@inheritDoc}
	 */
	public <R extends GrammarRule<T>> boolean areConstraintsSatisfied(int sourceRhsIndex, R rule) {

		// no further constraints are needed for a monolingual CFG
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public TokenSequence<T> getArcPackingString() {
		return packingString;
	}

	/**
	 * {@inheritDoc}
	 */
	public T getKeyPackingString() {
		return lhs;
	}

	public void incrementKeysCreated() {
		keysCreated++;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object other) {
		// each rule has a unique instance
		return (this == other);
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		// hash based on our reference
		return super.hashCode();
	}

	/**
	 * Gets a string representation of this <code>MonoCFGRule</code>.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		return lhs.getId() + " -> " + StringUtils.untokenize(rhs);
	}

	public int getKeysCreated() {
		return keysCreated;
	}
}
