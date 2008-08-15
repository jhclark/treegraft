package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.LinkedList;
import java.util.Queue;

/**
 * A temporary storage location for <code>Keys</code> that have been completed,
 * but that have not yet been processed to see how they affect the current state
 * of the <code>ActiveArcs</code>. The <code>ChartParser</code> will handle each
 * <code>Key</code> from the <code>Agenda</code> in turn and as each
 * <code>Key</code> is handled, it is removed from the <code>Agenda</code> and
 * placed into the <code>Chart</code>.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class Agenda<R extends GrammarRule<T>, T extends Token> {

	private final Queue<Key<R, T>> q = new LinkedList<Key<R, T>>();
	private final Chart<R, T> chart;

	public Agenda(Chart<R, T> chart) {
		this.chart = chart;
	}

	/**
	 * Checks to see if the <code>Agenda</code> is empty.
	 * 
	 * @return True if the agenda is empty; false otherwise.
	 */
	public boolean isEmpty() {
		return q.isEmpty();
	}

	public int size() {
		return q.size();
	}

	/**
	 * Adds a newly completed <code>ActiveArc</code> to the <code>Agenda</code>
	 * as a <code>Key</code> until it can be processed and added to the
	 * <code>Chart</code>; this method also handles the packing of ActiveArcs
	 * with the same LHS and different RHS's into a single Key.
	 * 
	 * @param completedArc
	 *            the arc to be added as a new key or packed into an existing
	 *            key
	 */
	public void addKeyToChartAndAgenda(ActiveArc<R, T> completedArc) {

		Key<R, T> newKey = null;

		// first, try to look up an existing key
		if (ActiveArcManager.DO_AMBIGUITY_PACKING) {
			// TODO: Grab key from cache here
		}

		// if no key was found, add a new one
		if (newKey == null) {
			newKey = new Key<R, T>(completedArc, null);
		} else {
			newKey.addCompletedArc(completedArc);
		}

		// assert q.contains(newKey) == false :
		// "Agenda already contains duplicate key: "
		// + newKey.toString();
		// assert chart.contains(newKey) == false :
		// "Chart already contains duplicate key: " + newKey;

		q.add(newKey);
		chart.addKey(newKey);

		// now do blame assignment
		blame(completedArc);
	}

	/**
	 * Recursively increment the counters of all rules that directly or
	 * indirectly caused the creation of this completed arc (key).
	 * 
	 * @param arc
	 */
	private void blame(ActiveArc<R, T> arc) {
		for (R rule : arc.getRules()) {
			rule.incrementKeysCreated();
		}
		for (int i = 0; i < arc.getRhs().length; i++) {
			for (Key<R, T> backpointer : arc.getBackpointers(i)) {
				for (ActiveArc<R, T> child : backpointer.getActiveArcs()) {
//					blame(child);
				}
			}
		}
	}

	/**
	 * Gets and removes from the <code>Agenda</code> the next <code>Key</code>
	 * to be processed in first-in, first-out (FIFO) order such that we perform
	 * a depth-first search (DFS).
	 * 
	 * @return
	 */
	public Key<R, T> get() {
		return q.remove();
	}

	public boolean contains(Key<R, T> key) {
		return q.contains(key);
	}

	/**
	 * Returns a string representation of this <code>Agenda</code> as a list of
	 * the <code>Keys</code> that are contained in it.
	 */
	public String toString() {
		return q.toString();
	}
}
