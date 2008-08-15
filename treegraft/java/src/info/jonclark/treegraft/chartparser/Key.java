package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.formatting.parses.Parse;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.scoring.ParseScorer;
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
	 * Gets the partial (or possibly complete) parses associated with this
	 * <code>Key</code> as formatted by the <code>ParseFormatter</code>.
	 * 
	 * @param formatter
	 *            an object that determines how the resulting parses will look
	 *            (including whether they are source trees, target trees, or
	 *            target strings).
	 * @return an array of formatted parses
	 */
	public List<Parse<R, T>> getPartialParses(ParseFormatter<R, T> formatter) {

		ArrayList<Parse<R, T>> result = new ArrayList<Parse<R, T>>();
		for (ActiveArc<R, T> arc : this.getActiveArcs()) {
			for (R rule : arc.getRules()) {
				result.addAll(unpackNonterminalBackpointers(this, arc, rule, formatter));
			}
		}
		return result;
	}

	private ArrayList<Parse<R, T>> unpackNonterminalBackpointers(Key<R, T> key,
			ActiveArc<R, T> arcToUnpack, R ruleToUnpack, ParseFormatter<R, T> formatter) {

		// iterate over all of the RHS constituents for this key
		int[] targetToSourceAlignment = formatter.getTargetToSourceRhsAlignment(key, ruleToUnpack);
		T[] transducedRhs = formatter.transduce(key, ruleToUnpack);
		ArrayList<Parse<R, T>>[] parsesFromBackpointer = new ArrayList[transducedRhs.length];

		// traverse left to right across transduced RHS
		// when we find a non-terminal, we will use the alignment
		// to map it back to the backpointers, which we can then
		// use to continue descending down the source-side backbone
		// structure

		for (int targetRhsIndex = 0; targetRhsIndex < transducedRhs.length; targetRhsIndex++) {

			if (transducedRhs[targetRhsIndex].isTerminal()) {
				parsesFromBackpointer[targetRhsIndex] =
						outputTerminal(formatter, transducedRhs[targetRhsIndex]);
			} else {

				int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
				List<Key<R, T>> backpointers = arcToUnpack.getBackpointers(sourceRhsIndex);
				assert backpointers != null : "null backpointer list at index " + sourceRhsIndex
						+ " for key " + key.toString();

				parsesFromBackpointer[targetRhsIndex] =
						outputNonterminal(key, ruleToUnpack, sourceRhsIndex, formatter,
								backpointers);
			}

		}

		ArrayList<Parse<R, T>> result =
				makeCrossProductOfParses(parsesFromBackpointer, formatter.getScorer());

		for (Parse<R, T> parse : result) {

			// update parse scores
			double nodeScore =
					formatter.getScorer().combineRuleScoreWithChildren(parse.getLogProb(),
							ruleToUnpack);
			parse.setCurrentScore(nodeScore);

			// open parentheses (or other nonterminal formatting)
			parse.prepend(formatter.formatNonterminalBefore(key, ruleToUnpack, nodeScore));

			// close parentheses (or other nonterminal formatting)
			parse.append(formatter.formatNonterminalAfter(key, ruleToUnpack, nodeScore));
		}

		return result;
	}

	private ArrayList<Parse<R, T>> outputTerminal(ParseFormatter<R, T> formatter, T terminal) {

		Parse<R, T> terminalParse = new Parse<R, T>();
		terminalParse.append(formatter.formatTerminal(terminal));

		ArrayList<Parse<R, T>> list = new ArrayList<Parse<R, T>>(1);
		list.add(terminalParse);
		return list;

	}

	private ArrayList<Parse<R, T>> outputNonterminal(Key<R, T> key, R parentRule,
			int sourceRhsIndex, ParseFormatter<R, T> formatter,
			List<Key<R, T>> nonterminalBackpointers) {

		ArrayList<Parse<R, T>> parsesFromNonterminal = new ArrayList<Parse<R, T>>();

		// since each backpointer is guaranteed to satisfy for the constraints
		// only of AT LEAST one rule, we must check to make sure that this
		// backpointer satisfies the constraints of the current rule before
		// adding it
		for (Key<R, T> backpointer : nonterminalBackpointers) {
			for (ActiveArc<R, T> backpointerArc : backpointer.getActiveArcs()) {
				for (R backpointerRule : backpointerArc.getRules()) {

					if (parentRule.areConstraintsSatisfied(sourceRhsIndex, backpointerRule)) {

						ArrayList<Parse<R, T>> parsesFromBackpointer =
								unpackNonterminalBackpointers(backpointer, backpointerArc,
										backpointerRule, formatter);
						parsesFromNonterminal.addAll(parsesFromBackpointer);
					}

				}
			}
		}

		return parsesFromNonterminal;
	}

	private ArrayList<Parse<R, T>> makeCrossProductOfParses(ArrayList<Parse<R, T>>[] parses,
			ParseScorer<R, T> scorer) {

		ArrayList<Parse<R, T>> currentResultParses = new ArrayList<Parse<R, T>>();
		Parse<R, T> blankParse = new Parse<R, T>();
		currentResultParses.add(blankParse);

		// take the crossproduct of the current parse with the new parses
		for (ArrayList<Parse<R, T>> parseList : parses) {

			ArrayList<Parse<R, T>> expandedResultParses =
					new ArrayList<Parse<R, T>>(currentResultParses.size() * parseList.size());

			for (Parse<R, T> parseFromBackpointer : parseList) {

				for (Parse<R, T> resultParse : currentResultParses) {
					Parse<R, T> expandedParse = new Parse<R, T>(resultParse);
					expandedParse.append(parseFromBackpointer);

					double newScore =
							scorer.combineChildScores(parseFromBackpointer.getLogProb(),
									resultParse.getLogProb());

					expandedParse.setCurrentScore(newScore);
					expandedResultParses.add(expandedParse);
				}
			}

			// get ready to tack on the next list of parses to this one
			currentResultParses = expandedResultParses;
		}

		return currentResultParses;
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
					+ getEndIndex() + ")" + "@" + super.hashCode();
		} else {
			return genId() + "=" + getLhs() + "(" + getStartIndex() + "," + getEndIndex() + ")"
					+ "@" + super.hashCode();
		}
	}
}
