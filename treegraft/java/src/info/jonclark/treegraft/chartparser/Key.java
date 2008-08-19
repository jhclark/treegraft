package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Stores information about an item that has already been proven to be a
 * complete parse. This class will be hashed by the Chart millions of times, so
 * an efficient <code>hashCode()</code> and <code>equals(Object)</code> method
 * is essential. This class is also one place where ambiguity UN-packing occurs
 * when final parses are being formatted via the <code>getPartialParses</code>
 * method.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class Key<R extends GrammarRule<T>, T extends Token> {

	private static final Logger log = LogUtils.getLogger();

	private final ArrayList<ActiveArc<R, T>> arcs = new ArrayList<ActiveArc<R, T>>();
	private final ActiveArc<R, T> firstArc;
	private final T word;
	private final int hashCode;

	// private double maxLogProb;

	/**
	 * Create a Key for a non-terminal or terminal grammar rule. This
	 * constructor is responsible for creating each key's score (log
	 * probability) based on the backpointers of its consituent keys.
	 * 
	 * @param arc
	 *            The completed <code>ActiveArc</code> that called for the
	 *            creation of this <code>Key</code>
	 * @param word
	 *            The terminal token associated with this key; null if this is a
	 *            non-terminal <code>Key</code>
	 */
	public Key(ActiveArc<R, T> arc, T word) {

		this.word = word;
		this.firstArc = arc;
		this.hashCode = genHash();
		if (arc != null) {
			this.arcs.add(arc);
		}
	}

	private int genHash() {

		int hashCode = 0;
		hashCode = firstArc.getLhs().hashCode();

		int mask = (firstArc.getStartIndex() << 16) & firstArc.getEndIndex();
		hashCode ^= mask;
		return hashCode;
	}

	public void addCompletedArc(ActiveArc<R, T> completedArc) {
		arcs.add(completedArc);
	}

	/**
	 * Determines if this <code>Key</code> is equal to another <code>Key</code>
	 * object.
	 * 
	 * @return True if the LHS's, start indices, and end indices match; false
	 *         otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj instanceof Key) {
			Key<R, T> key = (Key<R, T>) obj;
			return getLhs().equals(key.getLhs()) && getStartIndex() == key.getStartIndex()
					&& getEndIndex() == key.getEndIndex();
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode() {
		return hashCode;
	}

	/**
	 * Gets the zero-based source-side input index where this <code>Key</code>
	 * begins.
	 * 
	 * @return the start index
	 */
	public int getStartIndex() {
		return firstArc.getStartIndex();
	}

	/**
	 * Gets the zero-based source-side input index where this <code>Key</code>
	 * ends.
	 * 
	 * @return the end index
	 */
	public int getEndIndex() {
		return firstArc.getEndIndex();
	}

	/**
	 * @see GrammarRule.getLhs
	 */
	public T getLhs() {
		return firstArc.getLhs();
	}

	/**
	 * Gets the length of this key in terms of source-side tokens covered.
	 * 
	 * @return the length in tokens of this key
	 */
	public int getLength() {
		return (firstArc.getEndIndex() - firstArc.getStartIndex());
	}

	/**
	 * Gets the terminal token associated with this Key.
	 * 
	 * @return NULL for non-terminals; otherwise, a terminal <code>Token</code>
	 */
	public T getWord() {
		return word;
	}

	/**
	 * Gets the <code>ActiveArc</code> that created this <code>Key</code>.
	 * 
	 * @return NULL if this is a dummy key for a terminal token; otherwise, an
	 *         <code>ActiveArc</code>.
	 */
	public List<ActiveArc<R, T>> getActiveArcs() {
		return arcs;
	}

	/**
	 * Determine if this <code>Key</code> represents a terminal
	 * <code>Token</code>.
	 * 
	 * @return True if this is a terminal; False otherwise.
	 */
	public boolean isTerminal() {
		return (word != null);
	}

	/**
	 * Starts the grammar profiling timer for the rule associated with this key.
	 */
	public void startTimer() {
		// if (getRules() != null)
		// getRules().beginEvaluation();
	}

	/**
	 * Stops the grammar profiling timer for the rule associated with this key.
	 */
	public void stopTimer() {
		// if (getRules() != null)
		// getRules().stopEvaluation();
	}

	private String genId() {
		if (isTerminal() == false) {
			return getLhs().getId() + getStartIndex() + "." + getEndIndex();
		} else {
			return word.getId() + getStartIndex() + "." + getEndIndex();
		}
	}

	public T getKeyPackingString() {
		return firstArc.getKeyPackingString();
	}

	// /**
	// * Gets the score (in the log probability domain) associated with the best
	// * possible partial parse rooted at this <code>Key</code>.
	// *
	// * @return a log probability
	// */
	// public double getMaxLogProb() {
	// return maxLogProb;
	// }

	public boolean formsCycle(int depth) {
		return formsCycle(this, this, depth - 1);
	}

	private boolean formsCycle(Key<R, T> root, Key<R, T> currentKey, int depth) {

		for (ActiveArc<R, T> arc : currentKey.getActiveArcs()) {
			for (int i = 0; i < arc.getRhs().length; i++) {
				List<Key<R, T>> backpointers = arc.getBackpointers(i);
				if (backpointers != null) {
					for (Key<R, T> backpointer : backpointers) {
						if (backpointer.getLhs().equals(root.getLhs())) {
							log.warning("CYCLE DETECTED: " + root.toString() + " TO "
									+ currentKey.toString());
							return true;
						}
						if (depth > 0) {
							if (formsCycle(root, backpointer, depth - 1)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/**
	 * Gets a string representation of this <code>Key</code> including the rule
	 * or terminal symbol that lead to its creation, the source-side indices
	 * that it covers, and information from the <code>ActiveArc</code> that
	 * created it.
	 */
	public String toString() {
		if (word != null) {
			return genId() + "=<" + word.getId() + ">" + getLhs() + "(" + getStartIndex() + ","
					+ getEndIndex() + ")";
		} else {
			return genId() + "=" + getLhs() + "(" + getStartIndex() + "," + getEndIndex() + ")";
		}
	}
}
