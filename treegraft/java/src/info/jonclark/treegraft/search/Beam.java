package info.jonclark.treegraft.search;

import info.jonclark.lang.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * An n-best list of hypotheses.
 * 
 * @author jon
 */
public class Beam<T> implements Iterable<T> {

	private ArrayList<Double> scores;
	private ArrayList<T> hypotheses;
	private double worstScore = Double.POSITIVE_INFINITY;
	private double bestScore = Double.NEGATIVE_INFINITY;
	private int beamSize;

	/**
	 * @param beamSize
	 *            The size of the n-best list to be returned.
	 */
	public Beam(int beamSize) {
		this.scores = new ArrayList<Double>(beamSize);
		this.hypotheses = new ArrayList<T>(beamSize);
		this.beamSize = beamSize;
	}

	/**
	 * Adds an item to the beam. If this item's score is the same as a previous
	 * item, the previous item is overwritten
	 * 
	 * @param item
	 * @param score
	 */
	public void add(double score, T item) {
		if (hypotheses.size() < beamSize || score >= worstScore) {

			// search for insertion point
			int index = Collections.binarySearch(scores, score);
			index = Math.abs(index);

			// let worst item fall out of the beam, if needed
			if (hypotheses.size() == beamSize) {
				hypotheses.remove(0);
				scores.remove(0);
			}

			// insert new item and update scores
			if (index >= scores.size()) {
				scores.add(score);
				hypotheses.add(item);
			} else {
				scores.add(index, score);
				hypotheses.add(index, item);
			}
			worstScore = scores.get(0);
			bestScore = scores.get(scores.size() - 1);
		}
	}

	/**
	 * Get the best hypothesis currently in the beam (lower is better).
	 * 
	 * @return
	 */
	public T getBest() {
		if (hypotheses.size() == 0)
			return null;

		T best = hypotheses.get(hypotheses.size() - 1);
		return best;
	}

	/**
	 * Get entries in ranked order with entry 0 being the best
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Pair<Double, T>[] getAll() {
		Pair<Double, T>[] arr = (Pair<Double, T>[]) new Pair[hypotheses.size()];

		for (int i = scores.size() - 1; i >= 0; i--) {
			arr[i] = new Pair<Double, T>(scores.get(i), hypotheses.get(i));
		}

		return arr;
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

	public Iterator<T> iterator() {
		return hypotheses.iterator();
	}
}
