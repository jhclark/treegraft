package info.jonclark.treegraft.core.search;

import info.jonclark.treegraft.core.scoring.Scored;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * An n-best list of hypotheses.
 * 
 * @author jon
 */
public class Beam<H extends Scored> implements Iterable<H>, List<H> {

	private ArrayList<H> hypotheses;
	private double worstScore = Double.POSITIVE_INFINITY;
	private double bestScore = Double.NEGATIVE_INFINITY;
	private int beamSize;

	/**
	 * @param beamSize
	 *            The size of the n-best list to be returned.
	 */
	public Beam(int beamSize) {
		this.hypotheses = new ArrayList<H>(beamSize);
		this.beamSize = beamSize;
	}

	public Beam(List<H> other, int beamSize) {

		this.beamSize = beamSize;
		this.hypotheses = new ArrayList<H>(beamSize);

		if (other instanceof Beam) {
			Beam<H> otherBeam = (Beam<H>) other;
			this.hypotheses.addAll(otherBeam.hypotheses);
		} else {
			this.addAll(other);
		}
	}

	/**
	 * Adds an item to the beam. If this item's score is the same as a previous
	 * item, the previous item is overwritten
	 * 
	 * @param item
	 * @param score
	 */
	public boolean add(H item) {
		final double score = item.getLogProb();
		if (hypotheses.size() < beamSize || score >= worstScore) {

			// TODO: Reimplement this to make it faster since it's time critical
			// code

			// sort beam in ascending order
			// best hyp is at index n
			// worst is at index 0

			// search for insertion point using hacked up comparator that uses a
			// null key since we know the implementation detail that the binary
			// search always passes the score as the second argument to the
			// comparator
			// i.e. the second argument of binarySearch() would be passed in as
			// nulll in the comparator every time
			int index = Collections.binarySearch(hypotheses, null, new Comparator<H>() {
				public int compare(H hypFromList, H nulll) {
					if (score > hypFromList.getLogProb()) {
						return -1;
					} else if (score < hypFromList.getLogProb()) {
						return 1;
					} else {
						return 0;
					}
				}
			});
			if (index < 0) {
				index = -(index + 1);
			}

			// let worst item fall out of the beam, if needed
			if (hypotheses.size() == beamSize) {
				hypotheses.remove(0);
			}

			// insert new item and update scores
			if (index >= hypotheses.size()) {
				hypotheses.add(item);
			} else {
				hypotheses.add(index, item);
			}
			worstScore = hypotheses.get(0).getLogProb();
			bestScore = hypotheses.get(hypotheses.size() - 1).getLogProb();

			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get the best hypothesis currently in the beam (lower is better).
	 * 
	 * @return
	 */
	public H getBest() {
		if (hypotheses.size() == 0)
			return null;

		H best = hypotheses.get(hypotheses.size() - 1);
		return best;
	}

	public double getBestScore() {
		return bestScore;
	}

	public double getWorstScore() {
		return worstScore;
	}

	public int maxSize() {
		return beamSize;
	}

	public int currentSize() {
		return hypotheses.size();
	}

	public Iterator<H> iterator() {
		return hypotheses.iterator();
	}

	public void add(int index, H element) {
		throw new Error("Unsupported method.");
	}

	public boolean addAll(Collection<? extends H> c) {
		boolean changed = false;
		for (H h : c) {
			if (add(h)) {
				changed = true;
			}
		}
		return changed;
	}

	public boolean addAll(int index, Collection<? extends H> c) {
		throw new Error("Unsupported method.");
	}

	public void clear() {
		hypotheses.clear();
		worstScore = Double.POSITIVE_INFINITY;
		bestScore = Double.NEGATIVE_INFINITY;
	}

	public boolean contains(Object o) {
		return hypotheses.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		return hypotheses.containsAll(c);
	}

	public H get(int index) {
		return hypotheses.get(index);
	}

	public int indexOf(Object o) {
		return hypotheses.indexOf(o);
	}

	public boolean isEmpty() {
		return hypotheses.isEmpty();
	}

	public int lastIndexOf(Object o) {
		return hypotheses.lastIndexOf(o);
	}

	public ListIterator<H> listIterator() {
		throw new Error("Unsupported method.");
	}

	public ListIterator<H> listIterator(int index) {
		throw new Error("Unsupported method.");
	}

	public boolean remove(Object o) {
		throw new Error("Unsupported method.");
	}

	public H remove(int index) {
		throw new Error("Unsupported method.");
	}

	public boolean removeAll(Collection<?> c) {
		throw new Error("Unsupported method.");
	}

	public boolean retainAll(Collection<?> c) {
		throw new Error("Unsupported method.");
	}

	public H set(int index, H element) {
		throw new Error("Unsupported method.");
	}

	public int size() {
		return hypotheses.size();
	}

	public List<H> subList(int fromIndex, int toIndex) {
		return hypotheses.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		return hypotheses.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return hypotheses.toArray(a);
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(int i=hypotheses.size()-1; i >=0 ; i--) {
			builder.append((hypotheses.size()-i) + ") " + hypotheses.get(i) + "\n");
		}
		return builder.toString();
	}
}
