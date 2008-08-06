package info.jonclark.treegraft.core.formatting.parses;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.HashMap;

/**
 * Formats a the data contained in a <code>Key</code> into a partial (or
 * complete) parse, which could result in a source tree, target tree, or target
 * string. This abstract class allows for the separation of the complex
 * ambiguity unpacking algorithm and the mundane task of formatting a parse
 * tree.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public abstract class ParseFormatter<R extends GrammarRule<T>, T extends Token> {

	/**
	 * Specifies the type of the parse that will be constructed by this
	 * <code>ParseFormatter</code>.
	 */
	public enum OutputType {
		SOURCE_TREE, TARGET_TREE, TARGET_STRING
	}

	// TODO: Optimize this monotonic alignment map?
	private final static HashMap<Integer, int[]> monotonicAlignments =
			new HashMap<Integer, int[]>();

	/**
	 * Produces a string that should come before the children of the specified
	 * <code>Key</code>.
	 * 
	 * @param key
	 *            The key whose children are about to be unpacked
	 * @return a string to be appended to the parse being built
	 */
	public abstract String formatNonterminalBefore(Key<R, T> key);

	/**
	 * Produces a string that should come after the children of the specified
	 * <code>Key</code>.
	 * 
	 * @param key
	 *            The key whose children have just been unpacked
	 * @return a string to be appended to the parse being built
	 */
	public abstract String formatNonterminalAfter(Key<R, T> key);

	/**
	 * Produces a string that represents the given terminal token (which is
	 * usually the terminal itself).
	 * 
	 * @param key
	 *            The key whose children have just been unpacked
	 * @return a string to be appended to the parse being built
	 */
	public abstract String formatTerminal(T token);

	/**
	 * Gets the desired ordering of the RHS constituents for transduction. The
	 * format of these alignments is defined in {@link
	 * SyncCFGRule.getAlignment()}.
	 * 
	 * @param key
	 *            the key for which RHS alignments are desired
	 * @return an array with alignment information as defined in
	 *         <code>SyncCFGRule.getAlignment()</code>
	 */
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

	/**
	 * Gets an array in which the nth element has value n, which is useful in
	 * "transducing" from a monolingual parse to itself (the method by which we
	 * create source-side parse trees).
	 * 
	 * @param length
	 *            the desired length of the array
	 * @return an array that represents monotonic alignments
	 */
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