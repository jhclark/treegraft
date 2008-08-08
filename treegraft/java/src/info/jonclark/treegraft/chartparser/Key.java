package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.formatting.parses.Parse;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
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

	protected final ActiveArc<R, T> arc;
	private final T word;
	private final int hashCode;
//	private double maxLogProb;

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
		this.arc = arc;
		this.word = word;
		this.hashCode = genHash();

//		// TODO: extract this as a separate scoring function?
//		maxLogProb = arc.getRule().getLogProb();
//		if (this.isTerminal() == false) {
//			for (ArrayList<Key<R, T>> keys : arc.getBackpointers()) {
//				assert keys != null : "null backpointer list when creating key from active arc: "
//						+ arc.toString();
//				for (Key<R, T> key : keys) {
//					maxLogProb += key.maxLogProb;
//				}
//			}
//		}
	}

	private int genHash() {

		int hashCode = 0;
		hashCode = arc.getRule().getLhs().hashCode();

		int mask = (arc.getStartIndex() << 16) & arc.getEndIndex();
		hashCode ^= mask;
		return hashCode;
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
			return arc.getRule().getLhs().equals(key.arc.getRule().getLhs())
					&& arc.getStartIndex() == key.arc.getStartIndex()
					&& arc.getEndIndex() == key.getEndIndex();
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
		return arc.getStartIndex();
	}

	/**
	 * Gets the zero-based source-side input index where this <code>Key</code>
	 * ends.
	 * 
	 * @return the end index
	 */
	public int getEndIndex() {
		return arc.getEndIndex();
	}

	/**
	 * @see GrammarRule.getLhs
	 */
	public T getLhs() {
		return arc.getRule().getLhs();
	}

	/**
	 * Gets the length of this key in terms of source-side tokens covered.
	 * 
	 * @return the length in tokens of this key
	 */
	public int getLength() {
		return (arc.getEndIndex() - arc.getStartIndex());
	}

	/**
	 * Gets the <code>GrammarRule</code> that called for the creation of this
	 * <code>Key</code>.
	 * 
	 * @return NULL for terminals; otherwise, a <code>GrammarRule</code>
	 */
	public R getRule() {
		return arc.getRule();
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
	public ActiveArc<R, T> getActiveArc() {
		return arc;
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
		if (getRule() != null)
			getRule().beginEvaluation();
	}

	/**
	 * Stops the grammar profiling timer for the rule associated with this key.
	 */
	public void stopTimer() {
		if (getRule() != null)
			getRule().stopEvaluation();
	}

	private String genId() {
		if (arc.getRule() != null) {
			return arc.getRule().getLhs().getId() + arc.getStartIndex() + "." + arc.getEndIndex();
		} else {
			return word.getId() + arc.getStartIndex() + "." + arc.getEndIndex();
		}
	}

//	/**
//	 * Gets the score (in the log probability domain) associated with the best
//	 * possible partial parse rooted at this <code>Key</code>.
//	 * 
//	 * @return a log probability
//	 */
//	public double getMaxLogProb() {
//		return maxLogProb;
//	}

	public boolean formsCycle(int depth) {
		return formsCycle(this, this, depth - 1);
	}

	private boolean formsCycle(Key<R, T> root, Key<R, T> currentKey, int depth) {

		for (ArrayList<Key<R, T>> backpointers : currentKey.arc.getBackpointers()) {
			if (backpointers != null) {
				for (Key<R, T> backpointer : backpointers) {
					if (backpointer.getRule().getLhs().equals(root.getRule().getLhs())) {
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
	@SuppressWarnings("unchecked")
	public List<Parse<R, T>> getPartialParses(ParseFormatter<R, T> formatter) {

		ArrayList<Parse<R, T>> list = new ArrayList<Parse<R,T>>();
		list.add(new Parse<R,T>());

		unpackNonterminalBackpointers(this, list, formatter);

		return list;
	}

	private void unpackNonterminalBackpointers(Key<R, T> key, ArrayList<Parse<R,T>> currentList,
			ParseFormatter<R, T> formatter) {

		// iterate over all of the RHS constituents for this key
		int[] targetToSourceAlignment = formatter.getTargetToSourceRhsAlignment(key);
		T[] transducedRhs = formatter.transduce(key);
		ArrayList<Key<R, T>>[] backpointers = key.arc.getBackpointers();

		// traverse left to right across transduced RHS
		// when we find a non-terminal, we will use the alignment
		// to map it back to the backpointers, which we can then
		// use to continue descending down the source-side backbone
		// structure

		for (int targetRhsIndex = 0; targetRhsIndex < transducedRhs.length; targetRhsIndex++) {

			if (transducedRhs[targetRhsIndex].isTerminal()) {
				outputTerminal(currentList, formatter, transducedRhs[targetRhsIndex]);
			} else {

				int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
				if (sourceRhsIndex == -1) {
					log.severe("Error in rule: " + key.getRule().getRuleId()
							+ " @ "
							// + key.getRule().getFileAndLine().getName() + ":"
							// + key.getRule().getLineNumber()
							+ " -- target RHS non-terminal not aligned at position "
							+ targetRhsIndex);
				} else {

					assert backpointers[sourceRhsIndex] != null : "null backpointer list at index "
							+ sourceRhsIndex + " for key " + key.toString();
					outputNonterminal(key, currentList, formatter, backpointers[sourceRhsIndex]);
				}
			}

		}

		for (int i = 0; i < currentList.size(); i++) {
			
			// update parse scores
			Parse<R, T> parse = currentList.get(i);
			R rule = key.getRule();
			double nodeScore = formatter.getScorer().accumulate(parse.getCurrentLogProb(), rule);
			parse.setCurrentScore(nodeScore);
			
			// open parentheses (or other nonterminal formatting)
			parse.prependToParse(formatter.formatNonterminalBefore(key, nodeScore));

			// close parentheses (or other nonterminal formatting)
			parse.appendToParse(formatter.formatNonterminalAfter(key, nodeScore));
		}
	}

	private void outputTerminal(ArrayList<Parse<R,T>> currentList,
			ParseFormatter<R, T> formatter, T terminal) {

		for (int j = 0; j < currentList.size(); j++) {
			Parse<R,T> parse = currentList.get(j);
			parse.appendToParse(formatter.formatTerminal(terminal));
		}

	}

	private void outputNonterminal(Key<R, T> key, ArrayList<Parse<R,T>> currentList,
			ParseFormatter<R, T> formatter, ArrayList<Key<R, T>> nonterminalBackpointers) {

		// expand the list to the size of the pack
		int nAmbiguities = nonterminalBackpointers.size();
		ArrayList<Parse<R,T>>[] miniList = new ArrayList[nAmbiguities];

		// iterate over all of the ambiguities packed into this key
		// for the current RHS constituent
		for (int j = 0; j < nAmbiguities; j++) {
			miniList[j] = new ArrayList<Parse<R,T>>(currentList.size());
			for (final Parse<R,T> parse : currentList)
				miniList[j].add(new Parse<R,T>(parse));
			Key<R, T> backpointer = nonterminalBackpointers.get(j);

			unpackNonterminalBackpointers(backpointer, miniList[j], formatter);
		}

		// now that we've recursively expanded the miniList for the
		// relevant ambiguities add the miniList to the full list since it's now
		// safe from child recursive calls
		currentList.clear();
		for (int j = 0; j < nAmbiguities; j++) {
			currentList.addAll(miniList[j]);
		}
	}

	/**
	 * Gets a string representation of this <code>Key</code> including the rule
	 * or terminal symbol that lead to its creation, the source-side indices
	 * that it covers, and information from the <code>ActiveArc</code> that
	 * created it.
	 */
	public String toString() {
		if (word != null) {
			return genId() + "=<" + word.getId() + ">" + arc.toString();
		} else {
			return genId() + "=" + arc.toString();
		}
	}
}
