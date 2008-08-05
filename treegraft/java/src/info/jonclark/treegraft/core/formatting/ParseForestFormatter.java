package info.jonclark.treegraft.core.formatting;

import java.util.HashMap;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * @author jon
 * @param <R>
 * @param <T>
 * @param <F>
 *            Target return data type for ParseForest
 */
public abstract class ParseForestFormatter<R extends GrammarRule<T>, T extends Token, F> {

	// TODO: Optimize this monotonic alignment map?
	private final static HashMap<Integer, int[]> monotonicAlignments =
			new HashMap<Integer, int[]>();

	public abstract void addNonterminal(Key<R, T> key);

	public abstract void addTerminal(Key<R,T> key);

	public abstract int[] getRhsAlignment(Key<R, T> key);

	/**
	 * Given a key (which is associated with a GrammarRule), transduce its RHS
	 * to some array of terminals and non-terminals. In the case of a
	 * monolingual parse, this can simply be the original source RHS.
	 * 
	 * @param key
	 * @return
	 */
	public abstract T[] transduce(Key<R, T> key);

	public int[] getMonotonicAlignment(int length) {
		int[] alignment = monotonicAlignments.get(length);
		if (alignment == null) {
			alignment = new int[length];
			for (int i = 0; i < alignment.length; i++)
				alignment[i] = i;
		}
		return alignment;
	}
}
