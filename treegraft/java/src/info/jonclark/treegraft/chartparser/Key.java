package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.formatting.ParseForestFormatter;
import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;

/**
 * Stores information about an item that has already been proven to be a
 * complete parse. This class will be hashed by the Chart millions of times, so
 * an efficient hashCode() and equals(Object) method is essential. This class is
 * also where the ambiguity unpacking occurs when final parses are being
 * formatted.
 * 
 * @author jon
 * @param <R>
 */
public class Key<R extends GrammarRule<T>, T extends Token> {

	protected final ActiveArc<R, T> arc;
	private final T word;
	private final int hashCode;
	private double logProb;

	/**
	 * Create a Key for a non-terminal or terminal grammar rule. This
	 * constructor is responsible for creating each key's score (log
	 * probability) based on the backpointers of its consituent keys.
	 * 
	 * @param id
	 * @param arc
	 */
	public Key(ActiveArc<R, T> arc, T word) {
		this.arc = arc;
		this.word = word;
		this.hashCode = genHash();

		// TODO: extract this as a separate scoring function?
		logProb = arc.getRule().getLogProb();
		if (this.isTerminal() == false) {
			for (ArrayList<Key<R, T>> keys : arc.getBackpointers()) {
				assert keys != null : "null backpointer list when creating key from active arc: "
						+ arc.toString();
				for (Key<R, T> key : keys) {
					logProb += key.logProb;
				}
			}
		}
	}

	private int genHash() {

		int hashCode = 0;
		// if (arc.getGrammarRule() != null) {
		hashCode = arc.getRule().getLhs().hashCode();
		// } else {
		// hashCode = word.hashCode();
		// }

		int mask = (arc.getStartIndex() << 16) & arc.getEndIndex();
		hashCode ^= mask;
		return hashCode;
	}

	public boolean equals(Object obj) {
		// if (arc.getGrammarRule() != null) {
		if (obj instanceof Key) {
			Key<R, T> key = (Key<R, T>) obj;
			return arc.getRule().getLhs().equals(key.arc.getRule().getLhs())
					&& arc.getStartIndex() == key.arc.getStartIndex()
					&& arc.getEndIndex() == key.getEndIndex();
		} else {
			return false;
		}
		// } else {
		// return word.equals(key.word) && arc.getStartIndex() ==
		// key.arc.getStartIndex()
		// && arc.getEndIndex() == key.getEndIndex();
		// }
	}

	public int hashCode() {
		return hashCode;
	}

	public int getStartIndex() {
		return arc.getStartIndex();
	}

	public int getEndIndex() {
		return arc.getEndIndex();
	}

	public T getLhs() {
		return arc.getRule().getLhs();
	}

	public int getLength() {
		return (arc.getEndIndex() - arc.getStartIndex());
	}

	/**
	 * This will be NULL for terminals.
	 * 
	 * @return
	 */
	public R getRule() {
		return arc.getRule();
	}

	/**
	 * Gets the terminal token associated with this Key. This will be NULL for
	 * non-terminals.
	 * 
	 * @return
	 */
	public T getWord() {
		return word;
	}

	/**
	 * @return The active arc associated with this key or NULL if this is a
	 *         dummy key for a terminal token.
	 */
	public ActiveArc<R, T> getActiveArc() {
		return arc;
	}

	public boolean isTerminal() {
		return (word != null);
	}

	public void startTimer() {
		if (getRule() != null)
			getRule().beginEvaluation();
	}

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

	public double getLogProb() {
		return logProb;
	}

	@SuppressWarnings("unchecked")
	public Parse<R, T>[] getPartialParses(ParseFormatter<R, T> formatter) {

		ArrayList<StringBuilder> currentList = new ArrayList<StringBuilder>();
		currentList.add(new StringBuilder());

		unpackNonterminalBackpointers(this, currentList, formatter);

		Parse<R, T>[] result = (Parse<R, T>[]) new Parse[currentList.size()];
		for (int i = 0; i < currentList.size(); i++) {
			String str = currentList.get(i).toString().trim();
			result[i] = new Parse<R, T>(str);
			result[i].setRoot(this);
		}
		return result;
	}

	public <F> void getParseForest(ParseForestFormatter<R, T, F> formatter, F hypergraph) {
		throw new Error("Unimplemented.");
	}

	private void unpackNonterminalBackpointers(Key<R, T> key, ArrayList<StringBuilder> currentList,
			ParseFormatter<R, T> formatter) {

		// open parentheses (or other nonterminal formatting)
		for (int i = 0; i < currentList.size(); i++) {
			currentList.get(i).append(formatter.formatNonterminalBefore(key));
		}

		// TODO: Reverse the alignment direction

		// iterate over all of the RHS constituents for this key
		int[] alignment = formatter.getRhsAlignment(key);
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

				int sourceRhsIndex = alignment[targetRhsIndex];
				assert backpointers[sourceRhsIndex] != null : "null backpointer list at index "
						+ sourceRhsIndex + " for key " + key.toString();
				outputNonterminal(key, currentList, formatter, backpointers[sourceRhsIndex]);
			}

		}

		// close parentheses (or other nonterminal formatting)
		for (int i = 0; i < currentList.size(); i++) {
			currentList.get(i).append(formatter.formatNonterminalAfter(key));
		}
	}

	private void outputTerminal(ArrayList<StringBuilder> currentList,
			ParseFormatter<R, T> formatter, T terminal) {

		for (int j = 0; j < currentList.size(); j++) {
			StringBuilder sb = currentList.get(j);
			sb.append(formatter.formatTerminal(terminal));
		}

	}

	private void outputNonterminal(Key<R, T> key, ArrayList<StringBuilder> currentList,
			ParseFormatter<R, T> formatter, ArrayList<Key<R, T>> nonterminalBackpointers) {

		// expand the list to the size of the pack
		int nAmbiguities = nonterminalBackpointers.size();
		ArrayList<StringBuilder>[] miniList = new ArrayList[nAmbiguities];

		// iterate over all of the ambiguities packed into this key
		// for the current RHS constituent
		for (int j = 0; j < nAmbiguities; j++) {
			miniList[j] = new ArrayList<StringBuilder>(currentList.size());
			for (final StringBuilder sb : currentList)
				miniList[j].add(new StringBuilder(sb));
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

	public String toString() {
		if (word != null) {
			return genId() + "=<" + word.getId() + ">" + arc.toString();
		} else {
			return genId() + "=" + arc.toString();
		}
	}
}
