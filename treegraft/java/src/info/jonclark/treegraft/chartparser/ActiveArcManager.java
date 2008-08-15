package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;

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

	public static final boolean DO_AMBIGUITY_PACKING = true;
	private static final Logger log = LogUtils.getLogger();

	private final ProfilerTimer createNonterminalArcAmbiguityLookup;
	private final ProfilerTimer createNonterminalArcConstructor;
	private final ProfilerTimer createNonterminalArcAdd;

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
	private final HashMap<TokenSequence<T>, HashMap<T, ActiveArc<R, T>>>[][] packedArcs;
	private ArrayList<ActiveArc<R, T>> newlyCompletedArcs = new ArrayList<ActiveArc<R, T>>();

	private int size = 0;

	/**
	 * Creates a new <code>ActiveArcManager</code>.
	 * 
	 * @param inputSize
	 *            The length in tokens of the source input string to be parsed.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArcManager(int inputSize, ProfilerTimer parentTimer) {

		this.incompleteArcs = (HashMap<T, ArcGroup>[]) new HashMap[inputSize + 1];
		for (int i = 0; i < incompleteArcs.length; i++) {
			this.incompleteArcs[i] = new HashMap<T, ArcGroup>();
		}

		this.packedArcs = new HashMap[inputSize + 1][inputSize + 1];

		this.createNonterminalArcAmbiguityLookup =
				ProfilerTimer.newTimer("createNonterminalArcAmbiguityLookup", parentTimer, true,
						false);
		this.createNonterminalArcConstructor =
				ProfilerTimer.newTimer("createNonterminalArcConstructor", parentTimer, true, false);
		this.createNonterminalArcAdd =
				ProfilerTimer.newTimer("createNonterminalArcAdd", parentTimer, true, false);
	}

	private ActiveArc<R, T> getPackedArc(int startIndex, int endIndex, T neededConstituent,
			TokenSequence<T> packingString) {

		HashMap<TokenSequence<T>, HashMap<T, ActiveArc<R, T>>> packingStringToNeededConstituentMap =
				packedArcs[startIndex][endIndex];
		if (packingStringToNeededConstituentMap == null) {
			return null;
		} else {
			HashMap<T, ActiveArc<R, T>> neededConstituentMap =
					packingStringToNeededConstituentMap.get(packingString);
			if (neededConstituentMap == null) {
				return null;
			} else {
				ActiveArc<R, T> arc = neededConstituentMap.get(neededConstituent);
				if (arc == null) {
					return null;
				} else {
					assert arc.getStartIndex() == startIndex : "Start index mismatch";
					assert arc.getEndIndex() == endIndex : "End index mismatch";
					return arc;
				}
			}
		}
	}

	/**
	 * Creates a new <code>ActiveArc</code> for a dummy terminal-initial rule
	 * 
	 * @param i
	 * @param lexicalRule
	 * @return
	 */
	public ActiveArc<R, T> createTerminalArc(int i, R lexicalRule, Key<R, T> dummyKey) {

		ActiveArc<R, T> ruleArc = null;
		if (DO_AMBIGUITY_PACKING) {
			T nextNeed = null;
			if (lexicalRule.getRhs().length > 1) {
				nextNeed = lexicalRule.getRhs()[0];
			}
			ruleArc = getPackedArc(i, i + 1, nextNeed, lexicalRule.getArcPackingString());
		}

		if (ruleArc == null) {
			ruleArc = new ActiveArc<R, T>(i, i + 1, 1, lexicalRule);
			ruleArc.addBackpointer(0, dummyKey);
			add(ruleArc);
		} else {
			// if we already have an arc here, then it can only have been
			// triggered by the same lexical item for which a backpointer
			// already exists, so there is no reason to add another
			// backpointer?????
			ruleArc.addRule(lexicalRule);
		}

		return ruleArc;
	}

	public void createNonterminalArc(Key<R, T> key, R rule) {

		ActiveArc<R, T> arc = null;

		createNonterminalArcAmbiguityLookup.go();
		if (DO_AMBIGUITY_PACKING) {
			// first, see if we already have an arc like this that we can pack
			T nextNeed = null;
			if (rule.getRhs().length > 1) {
				nextNeed = rule.getRhs()[1];
			}
			arc =
					getPackedArc(key.getStartIndex(), key.getEndIndex(), nextNeed,
							rule.getArcPackingString());
		}
		createNonterminalArcAmbiguityLookup.pause();

		if (arc == null) {
			// create arc with dot after the first RHS constituent
			createNonterminalArcConstructor.go();
			arc = new ActiveArc<R, T>(key.getStartIndex(), key.getEndIndex(), 1, rule);
			arc.addBackpointer(0, key);
			createNonterminalArcConstructor.pause();

			createNonterminalArcAdd.go();
			add(arc);
			createNonterminalArcAdd.pause();
		} else {

			// if the arc already exists, then it already has a
			// backpointer??????
			arc.addRule(rule);
		}
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

		// first, the active arc is filed by its start and end indices in the 2d
		// array

		// next, we file it by the rule-specific packing string (usually the LHS
		// + RHS)
		HashMap<TokenSequence<T>, HashMap<T, ActiveArc<R, T>>> packingStringToNeededConstituentMap =
				packedArcs[arc.getStartIndex()][arc.getEndIndex()];

		if (packingStringToNeededConstituentMap == null) {
			packingStringToNeededConstituentMap =
					new HashMap<TokenSequence<T>, HashMap<T, ActiveArc<R, T>>>();
			packedArcs[arc.getStartIndex()][arc.getEndIndex()] =
					packingStringToNeededConstituentMap;
		}

		// finally, we file it by which constituent it needs next
		HashMap<T, ActiveArc<R, T>> neededConstituentMap =
				packingStringToNeededConstituentMap.get(arc.getNeededSymbol());

		if (neededConstituentMap == null) {
			neededConstituentMap = new HashMap<T, ActiveArc<R, T>>();
			packingStringToNeededConstituentMap.put(arc.getArcPackingString(), neededConstituentMap);
			packedArcs[arc.getStartIndex()][arc.getEndIndex()] =
					packingStringToNeededConstituentMap;
		}
		neededConstituentMap.put(arc.getNeededSymbol(), arc);

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

		// find any active arcs that need this Key's LHS to advance further
		int j = key.getStartIndex();
		T needs = key.getLhs();
		ArcGroup affectedArcs = incompleteArcs[j].get(needs);

		if (affectedArcs != null) {

			for (final ActiveArc<R, T> affectedArc : affectedArcs.list) {
				assert key.getLhs().equals(affectedArc.getRhs()[affectedArc.getDot()]) : "Key cannot expand this rule: LHS mismatch (key:"
						+ key.toString() + " arc:" + affectedArc.toString() + ")";

				if (affectedArc.areConstraintsSatisfied(affectedArc.getDot(), key)) {

					ActiveArc<R, T> extendedArc = null;
					if (DO_AMBIGUITY_PACKING) {
						T neededSymbol = null;
						if (affectedArc.getDot() + 2 < affectedArc.getRhs().length) {
							neededSymbol = affectedArc.getRhs()[affectedArc.getDot() + 1];
						}
						extendedArc =
								getPackedArc(affectedArc.getStartIndex(), key.getEndIndex(),
										neededSymbol, affectedArc.getArcPackingString());
					}

					if (extendedArc == null) {
						// there was no existing arc, so add a new one
						extendedArc = affectedArc.extend(key);
						add(extendedArc);
					} else {
						// if the arc already exists, then we've already added
						// the key with the same LHS and span as a backpointer
						// to this arc
						
						// extendedArc.addBackpointer(affectedArc.getDot(),
						// key);
					}
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
