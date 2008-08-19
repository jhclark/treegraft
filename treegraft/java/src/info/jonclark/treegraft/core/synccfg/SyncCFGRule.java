package info.jonclark.treegraft.core.synccfg;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.forestunpacking.parses.BasicTreeFormatter;
import info.jonclark.treegraft.core.monocfg.MonoCFGRule;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.RuleException;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.treegraft.util.ProbUtils;
import info.jonclark.util.StringUtils;

import java.io.File;

/**
 * A synchronous version of a Context-Free GrammarRule.
 * 
 * @see SyncCFGRuleFactory
 * @see BasicTreeFormatter
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class SyncCFGRule<T extends Token> implements GrammarRule<T> {

	private final T lhs;
	private final T[] rhs;
	private final T targetLhs;
	private final T[] targetRhs;

	private int keysCreated = 0;

	private final TokenSequence<T> packingString;

	private final int[] sourceToTargetAlignment;
	private final int[] targetToSourceAlignment;
	private final double logProb;

	private final String ruleId;

	// private final File file;
	// private final int lineNumber;

	// private final Constraint[] constraints;
	private final SecondTimer cost = new SecondTimer(false, false);

	/**
	 * Creates a new <code>SyncCFGRule</code>.
	 * 
	 * @param lhs
	 * @param rhs
	 * @param targetLhs
	 * @param targetRhs
	 * @param ruleName
	 * @param ruleId
	 * @param targetToSourceAlignment
	 * @param prob
	 *            A score in the probability domain (internally, it will be
	 *            converted to log prob)
	 * @param constraints
	 * @param file
	 * @param lineNumber
	 * @throws RuleException
	 */
	public SyncCFGRule(T lhs, T[] rhs, T targetLhs, T[] targetRhs, String ruleId,
			int[] targetToSourceAlignment, double prob, Constraint[] constraints, File file,
			int lineNumber, TokenFactory<T> tokenFactory) throws RuleException {

		this.lhs = lhs;
		this.rhs = rhs;
		this.targetLhs = targetLhs;
		this.targetRhs = targetRhs;
		this.ruleId = ruleId;
		this.targetToSourceAlignment = targetToSourceAlignment;
		this.logProb = ProbUtils.logProb(prob);

		this.packingString = MonoCFGRule.makePackingString(lhs, rhs, tokenFactory);

		// invert the alignments
		this.sourceToTargetAlignment = new int[rhs.length];
		for (int i = 0; i < sourceToTargetAlignment.length; i++) {
			sourceToTargetAlignment[i] = -1;
		}
		for (int i = 0; i < targetToSourceAlignment.length; i++) {
			if (targetToSourceAlignment[i] != -1) {
				sourceToTargetAlignment[targetToSourceAlignment[i]] = i;
			}
		}

		// if this isn't a dummy rule
		if (targetRhs != null) {

			// Check to make sure alignments are only between non-terminals and
			// that
			// all non-terminals are aligned
			for (int i = 0; i < sourceToTargetAlignment.length; i++) {
				if (sourceToTargetAlignment[i] == -1) {
					if (rhs[i].isTerminal() == false) {
						throw new RuleException("Source RHS non-terminal unaligned at position "
								+ (i + 1) + " for rule " + this.toString());
					}
				} else {
					int n = sourceToTargetAlignment[i];
					if (targetRhs[n].isTerminal() == true) {
						throw new RuleException("Target RHS terminal aligned at position "
								+ (n + 1) + " (terminals cannot be aligned)" + " for rule "
								+ this.toString());
					}
				}
			}

			for (int i = 0; i < targetToSourceAlignment.length; i++) {
				if (targetToSourceAlignment[i] == -1) {
					if (targetRhs[i].isTerminal() == false) {
						throw new RuleException("Target RHS non-terminal unaligned at position "
								+ (i + 1) + " for rule " + this.toString());
					}
				} else {
					int n = targetToSourceAlignment[i];
					if (rhs[n].isTerminal() == true) {
						throw new RuleException("Source RHS terminal aligned at position "
								+ (n + 1) + " (terminals cannot be aligned)" + " for rule "
								+ this.toString());
					}
				}
			}
		}

		// this.file = file;
		// this.lineNumber = lineNumber;
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
	public double getTimeCost() {
		return cost.getSeconds();
	}

	// /**
	// * {@inheritDoc}
	// */
	// public Constraint[] getConstraints() {
	// return constraints;
	// }

	/**
	 * {@inheritDoc}
	 */
	public String getRuleId() {
		return ruleId;
	}

	/**
	 * {@inheritDoc}
	 */
	// public File getFile() {
	// return file;
	// }
	/**
	 * {@inheritDoc}
	 */
	// public int getLineNumber() {
	// return lineNumber;
	// }
	/**
	 * {@inheritDoc}
	 */
	public double getLogProb() {
		return logProb;
	}

	/**
	 * Returns the alignment as an int array having the same length as the
	 * target RHS, where each index corresponds to a target constituent and each
	 * value corresponds to a source constituent. An array index containing the
	 * value -1 indicates that the source constituent is unaligned.
	 * 
	 * @return
	 */
	public int[] getTargetToSourceAlignment() {
		assert targetToSourceAlignment.length == targetRhs.length : "array length mismatch";
		return targetToSourceAlignment;
	}

	/**
	 * Returns the alignment as an int array having the same length as the
	 * source RHS, where each index corresponds to a source constituent and each
	 * value corresponds to a target constituent. An array index containing the
	 * value -1 indicates that the target constituent is unaligned.
	 * 
	 * @return
	 */
	public int[] getSourceToTargetAlignment() {
		assert sourceToTargetAlignment.length == rhs.length : "array length mismatch";
		return sourceToTargetAlignment;
	}

	/**
	 * Checks to make sure the target side non-terminals are of the same type
	 * before allowing a key to extend an <code>ActiveArc</code>.
	 * 
	 * @param sourceRhsIndex
	 *            the source-side RHS rule index that is about to be extended in
	 *            an <code>ActiveArc</code>
	 * @param key
	 *            a rule from the proposed key to extend that arc
	 */
	public <R extends GrammarRule<T>> boolean areConstraintsSatisfied(int sourceRhsIndex,
			R ruleFromKey) {

		SyncCFGRule<T> syncRuleFromKey = (SyncCFGRule<T>) ruleFromKey;
		if (rhs[sourceRhsIndex].isTerminal()) {
			// terminals are always okay
			return true;
		} else {
			int targetRhsIndex = sourceToTargetAlignment[sourceRhsIndex];

			T requiredNonterminal = targetRhs[targetRhsIndex];
			T actualNonterminal = syncRuleFromKey.targetLhs;

			// for a synchronous CFG rule, we have to make sure the target
			// non-terminal symbols match
			return requiredNonterminal.equals(actualNonterminal);
		}
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

	private int hash = -1;

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		if (hash == -1) {
			// hash based on our reference
			hash = super.hashCode();
		}
		return hash;
	}

	/**
	 * Gets a string representation of this <code>SyncCFGRule</code>.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		return ruleId
				+
				// "@" + file.getName() + ":" + lineNumber +
				" = " + lhs + "::" + targetLhs + " : [ " + StringUtils.untokenize(rhs) + " ] -> [ "
				+ StringUtils.untokenize(targetRhs) + " ]";
	}

	public int getKeysCreated() {
		return keysCreated;
	}
}
