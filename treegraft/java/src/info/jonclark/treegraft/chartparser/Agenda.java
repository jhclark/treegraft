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

	/**
	 * Checks to see if the <code>Agenda</code> is empty.
	 * 
	 * @return True if the agenda is empty; false otherwise.
	 */
	public boolean isEmpty() {
		return q.isEmpty();
	}

	/**
	 * Adds a newly completed <code>Key</code> to the <code>Agenda</code> until
	 * it can be processed and added to the <code>Chart</code>.
	 * 
	 * @param key
	 *            the key to be added
	 */
	public void add(Key<R, T> key) {
		q.add(key);
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

	/**
	 * Returns a string representation of this <code>Agenda</code> as a list of
	 * the <code>Keys</code> that are contained in it.
	 */
	public String toString() {
		return q.toString();
	}
}
