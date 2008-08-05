package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles extending arcs based on incoming keys and storing partially completed
 * active arcs. All completed arcs are handed off to the Chart.
 * 
 * @author jon
 * @param <R>
 * @param <T>
 */
public class ActiveArcManager<R extends GrammarRule<T>, T extends Token> {

	/**
	 * Initial ArrayList capacity where potential ambiguities may be packed.
	 */
	public static final int DEFAULT_PACKING_SIZE = 5;

	private class PackedArc {
		// TODO: exchange ArrayList for a scored beam
		public ArrayList<ActiveArc<R, T>> list =
				new ArrayList<ActiveArc<R, T>>(DEFAULT_PACKING_SIZE);
	}

	private final HashMap<T, PackedArc>[] activeArcs;
	private ArrayList<ActiveArc<R, T>> newlyCompletedArcs = new ArrayList<ActiveArc<R, T>>();

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
		T needs = arc.getNeededSymbol();

		if (needs != null) {
			// store this arc indexed by the next constituent it needs
			append(activeArcs[j], needs, arc);
			System.out.println(" which will be needed at position " + arc.getEndIndex());
		} else {
			// no more constituents needed
			newlyCompletedArcs.add(arc);
			
			System.out.println("ADDING COMPLETED ARC: " + arc);
		}
	}

	/**
	 * Append an arc to a packed arc list for the map associated with a
	 * particular input span.
	 * 
	 * @param map
	 * @param token
	 * @param value
	 */
	private void append(Map<T, PackedArc> map, T token, ActiveArc<R, T> value) {
		
		PackedArc packedArc = map.get(token);
		if (packedArc == null) {
			packedArc = new PackedArc();
			map.put(token, packedArc);

			System.out.print("ADDING NEW ARC: " + token);
		} else {
			System.out.print("APPENDING ARC: " + token);
		}
		packedArc.list.add(value);
	}

	/**
	 * Advance the "dots" that iterates over the RHS's of arcs based on a new
	 * key.
	 * 
	 * @return
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

	@SuppressWarnings("unchecked")
	public ActiveArc<R, T>[] getAndClearCompletedArcs() {
		ActiveArc<R, T>[] arr =
				newlyCompletedArcs.toArray((ActiveArc<R, T>[]) new ActiveArc[newlyCompletedArcs.size()]);
		newlyCompletedArcs.clear();
		return arr;
	}
}
