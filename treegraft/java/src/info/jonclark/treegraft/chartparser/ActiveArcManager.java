package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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

	private static final Logger log = LogUtils.getLogger();

	/**
	 * Initial ArrayList capacity where potential ambiguities may be packed.
	 * Increasing this value will take up more memory initially, but will reduce
	 * the amount of time spent growing the ambiguity vectors if the number of
	 * ambiguities created during parsing typically exceeds this number.
	 */
	public static final int DEFAULT_PACKING_SIZE = 5;

	/**
	 * A wrapper for ambiguity lists that handle the packing of arcs. See also
	 * Chart.PackedKey
	 */
	private class ArcGroup {
		// TODO: exchange ArrayList for a scored beam
		public ArrayList<ActiveArc<R, T>> list =
				new ArrayList<ActiveArc<R, T>>(DEFAULT_PACKING_SIZE);
	}

	private final HashMap<T, ArcGroup>[] incompleteArcs;
	private final HashMap<R, ActiveArc<R, T>>[] initialArcs;
	private ArrayList<ActiveArc<R, T>> newlyCompletedArcs = new ArrayList<ActiveArc<R, T>>();

	private int size = 0;

	/**
	 * Creates a new <code>ActiveArcManager</code>.
	 * 
	 * @param inputSize
	 *            The length in tokens of the source input string to be parsed.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArcManager(int inputSize) {

		this.incompleteArcs = (HashMap<T, ArcGroup>[]) new HashMap[inputSize + 1];
		for (int i = 0; i < incompleteArcs.length; i++) {
			this.incompleteArcs[i] = new HashMap<T, ArcGroup>();
		}

		this.initialArcs = new HashMap[inputSize + 1];
		for (int i = 0; i < initialArcs.length; i++) {
			this.initialArcs[i] = new HashMap<R, ActiveArc<R, T>>();
		}
	}

	/**
	 * Creates a new <code>ActiveArc</code> for a dummy terminal-initial rule
	 * 
	 * @param i
	 * @param lexicalRule
	 * @return
	 */
	public ActiveArc<R, T> createTerminalArc(int i, R lexicalRule) {
		ActiveArc<R, T> ruleArc = new ActiveArc<R, T>(i, i + 1, 1, lexicalRule);
		add(ruleArc);
		return ruleArc;
	}

	public ActiveArc<R, T> createNonterminalArc(Key<R, T> key, R rule) {

		ActiveArc<R, T> arc;
		
		// first, see if we already have an arc like this that we can pack
		arc = initialArcs[key.getStartIndex()].get(rule);

		if (arc == null) {
			// create arc with dot after the first RHS constituent
			arc = new ActiveArc<R, T>(key.getStartIndex(), key.getEndIndex(), 1, rule);
			initialArcs[key.getStartIndex()].put(rule, arc);
			add(arc);
		}
		return arc;
	}

	/**
	 * Adds an arc that has just been extended in some way. It is the caller's
	 * responsibility to ensure that any necessary backpointers are added to the
	 * ActiveArc.
	 * 
	 * @param arc
	 */
	private void add(ActiveArc<R, T> arc) {
		int j = arc.getEndIndex();
		T neededSymbol = arc.getNeededSymbol();

		if (neededSymbol != null) {
			// store this arc indexed by the next constituent it needs
			append(incompleteArcs[j], neededSymbol, arc);
		} else {
			// no more constituents needed
			newlyCompletedArcs.add(arc);

			log.fine("ADDING COMPLETED ARC: " + arc);
		}

		size++;
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
	private void append(Map<T, ArcGroup> map, T neededTerminalOrNonterminal, ActiveArc<R, T> value) {

		ArcGroup packedArc = map.get(neededTerminalOrNonterminal);
		if (packedArc == null) {
			packedArc = new ArcGroup();
			map.put(neededTerminalOrNonterminal, packedArc);

			log.fine("ADDING NEW ARC: " + value);
		} else {
			log.fine("APPENDING ARC: " + value);
		}
		packedArc.list.add(value);
	}

	/**
	 * Advance the "dots" that iterates over the source side RHS's of arcs based
	 * on a new key. We only advance the dot if the candidate key meets the
	 * constraints enforced by the arc's governing rule as defined in
	 * <code>GrammarRule.areConstraintsSatisfied()</code>. This method is used
	 * by <code>ChartParser.parse</code> to extend the arcs using terminals
	 * symbols from the input.
	 */
	public void extendArcs(Key<R, T> key) {

		// NOTE: ambiguity packing occurs here (and when keys are added)

		int j = key.getStartIndex();
		T needs = key.getLhs();
		ArcGroup affectedArcs = incompleteArcs[j].get(needs);

		if (affectedArcs != null) {
			for (final ActiveArc<R, T> arc : affectedArcs.list) {
				assert key.getLhs().equals(arc.getRule().getRhs()[arc.getDot()]) : "Key cannot expand this rule: LHS mismatch (key:"
						+ key.toString() + " arc:" + arc.toString() + ")";

				if (arc.getRule().areConstraintsSatisfied(arc.getDot(), key)) {

					// there was no existing arc, so add a new one
					ActiveArc<R, T> extendedArc = arc.extend(key);
					add(extendedArc);
				}
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

	/**
	 * Gets the number of <code>ActiveArcs</code> that were created during
	 * parsing.
	 * 
	 * @return the size of the active arc list
	 */
	public int size() {
		return size;
	}
}
