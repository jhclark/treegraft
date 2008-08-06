package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles extending arcs based on incoming {@link Key}s and storing partially
 * completed {@link ActiveArc}s. All completed arcs are handed off to the
 * {@link Chart}.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class ActiveArcManager<R extends GrammarRule<T>, T extends Token> {

	/**
	 * Initial ArrayList capacity where potential ambiguities may be packed.
	 * Increasing this value will take up more memory initially, but will reduce
	 * the amount of time spent growing the ambiguity vectors if the number of
	 * ambiguities created during parsing typically exceeds this number.
	 */
	public static final int DEFAULT_PACKING_SIZE = 5;

	/**
	 * A wrapper for ambiguity lists that handle the packing of arcs. See also Chart.PackedKey
	 */
	private class PackedArc {
		// TODO: exchange ArrayList for a scored beam
		public ArrayList<ActiveArc<R, T>> list =
				new ArrayList<ActiveArc<R, T>>(DEFAULT_PACKING_SIZE);
	}

	private final HashMap<T, PackedArc>[] activeArcs;
	private ArrayList<ActiveArc<R, T>> newlyCompletedArcs = new ArrayList<ActiveArc<R, T>>();

	/**
	 * Creates a new <code>ActiveArcManager</code>.
	 * 
	 * @param inputSize
	 *            The length in tokens of the source input string to be parsed.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArcManager(int inputSize) {

		this.activeArcs = (HashMap<T, PackedArc>[]) new HashMap[inputSize + 1];
		for (int i = 0; i < activeArcs.length; i++) {
			this.activeArcs[i] = new HashMap<T, PackedArc>();
		}
	}

	/**
	 * Adds an arc that has just been extended in some way. It is the caller's
	 * responsibility to ensure that any necessary backpointers are added to the
	 * ActiveArc.
	 * 
	 * @param arc
	 */
	public void add(ActiveArc<R, T> arc) {
		int j = arc.getEndIndex();
		T neededSymbol = arc.getNeededSymbol();

		if (neededSymbol != null) {
			// store this arc indexed by the next constituent it needs
			append(activeArcs[j], neededSymbol, arc);
			System.out.println(" which will be needed at position " + arc.getEndIndex());
		} else {
			// no more constituents needed
			newlyCompletedArcs.add(arc);

			System.out.println("ADDING COMPLETED ARC: " + arc);
		}
	}

	/**
	 * Append an arc to a packed arc list for the map associated with a
	 * particular input span. Arcs are index by both the position that will
	 * provide its next needed constituent and the constituent that is needed
	 * (either a terminal symbol or a non-terminal type).
	 * 
	 * @param map
	 *            The map of constituents for the position at which the
	 *            constituent will be needed.
	 * @param neededTerminalOrNonterminal
	 * @param value
	 */
	private void append(Map<T, PackedArc> map, T neededTerminalOrNonterminal, ActiveArc<R, T> value) {

		PackedArc packedArc = map.get(neededTerminalOrNonterminal);
		if (packedArc == null) {
			packedArc = new PackedArc();
			map.put(neededTerminalOrNonterminal, packedArc);

			System.out.print("ADDING NEW ARC: " + neededTerminalOrNonterminal);
		} else {
			System.out.print("APPENDING ARC: " + neededTerminalOrNonterminal);
		}
		packedArc.list.add(value);
	}

	/**
	 * Advance the "dots" that iterates over the source side RHS's of arcs based
	 * on a new key. This method is used by <code>ChartParser.parse</code> to
	 * extend the arcs using terminals symbols from the input.
	 */
	public void extendArcs(Key<R, T> key) {

		// NOTE: ambiguity packing occurs here (and when keys are added)

		int j = key.getStartIndex();
		T needs = key.getLhs();
		PackedArc affectedArcs = activeArcs[j].get(needs);

		if (affectedArcs != null) {
			for (final ActiveArc<R, T> arc : affectedArcs.list) {
				assert key.getLhs().equals(arc.getRule().getRhs()[arc.getDot()]) : "Key cannot expand this rule: LHS mismatch (key:"
						+ key.toString() + " arc:" + arc.toString() + ")";

				ActiveArc<R, T> extendedArc = arc.extend(key);
				add(extendedArc);
			}
		}
	}

	/**
	 * Gets an array of <code>ActiveArcs</code> that were completed since the
	 * last call to this method.
	 * 
	 * @return an array of <code>ActiveArcs</code>
	 */
	@SuppressWarnings("unchecked")
	public ActiveArc<R, T>[] getAndClearCompletedArcs() {
		ActiveArc<R, T>[] arr =
				newlyCompletedArcs.toArray((ActiveArc<R, T>[]) new ActiveArc[newlyCompletedArcs.size()]);
		newlyCompletedArcs.clear();
		return arr;
	}
}
