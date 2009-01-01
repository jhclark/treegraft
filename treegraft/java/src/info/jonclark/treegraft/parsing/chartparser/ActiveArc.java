package info.jonclark.treegraft.parsing.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Represents the current state of a dotted grammar rule in the
 * <code>ChartParser</code>. The collection of all current
 * <code>ActiveArc</code>s is managed by the {@link ActiveArcManager}. In terms
 * of theorm-proving, when an <code>ActiveArc</code> is completed, it means the
 * constituent exists, and so it is then converted into a {@link Key} by the
 * <code>ChartParser</code> and stored in the {@link Chart}.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class ActiveArc<R extends GrammarRule<T>, T extends Token> {

	private static final Logger log = LogUtils.getLogger();

	/**
	 * The initial list size for the backpointer lists. Increasing this value
	 * will take up more memory initially, but will reduce the amount of time
	 * spent growing the backpointer vectors if the number of backpointers
	 * created during parsing typically exceeds this number.
	 */
	public static final int DEFAULT_BACKPOINTER_LIST_SIZE = 1;
	public static final int DEFAULT_RULE_LIST_SIZE = 5;

	private final int data;
	// private final byte startIndex;
	// private final byte endIndex;
	// private final byte dot;
	private HashSet<R> rules;
	private R firstRule;
	private ArrayList<Key<R, T>> backpointersForCurrentDot =
			new ArrayList<Key<R, T>>(DEFAULT_BACKPOINTER_LIST_SIZE);
	private ActiveArc<R, T>[] parentArcs;

	static long totalElements = 0;
	static long totalLists = 0;

	/**
	 * Creates a new <code>ActiveArc</code>.
	 * 
	 * @param startIndex
	 *            The zero-based source input index where this arc begins.
	 * @param endIndex
	 *            The zero-based source input index where this arc currently
	 *            ends, given the dot position.
	 * @param dot
	 *            The number of RHS consituents in this <code>ActiveArc</code>'s
	 *            rule that have been covered so far.
	 * @param firstRule
	 *            The <code>GrammarRule</code> that called for the creation of
	 *            this <code>ActiveArc</code> given the current state of the
	 *            <code>Chart</code>.
	 */
	public ActiveArc(int startIndex, int endIndex, int dot, R firstRule) {

		this(startIndex, endIndex, dot, null, null, null, null);
		this.rules = new HashSet<R>(DEFAULT_RULE_LIST_SIZE);
		rules.add(firstRule);
		this.firstRule = firstRule;
	}

	public ActiveArc(int startIndex, int endIndex, int dot, R firstRule, HashSet<R> rules,
			ActiveArc<R, T>[] prevParentArcs, ActiveArc<R, T> parentArc) {

		totalLists++;

		this.rules = rules;
		this.firstRule = firstRule;

		// since active arcs don't always cover the same spans,
		// sharing backpointer lists would be a bad idea
		// however, the *parents* of this ActiveArc
		// that cover the same spans *before* this dot
		// are eligible for backpointer sharing
		if (parentArc != null) {
			this.parentArcs = new ActiveArc[dot - 1];
			this.parentArcs[dot - 2] = parentArc;
			if (prevParentArcs != null) {
				System.arraycopy(prevParentArcs, 0, this.parentArcs, 0, prevParentArcs.length);
			}
		}

		// store all the data in a single int to save memory
		endIndex <<= 8;
		dot <<= 16;
		this.data = startIndex | endIndex | dot;

		// \/ WARNING: VALUES OF ENDINDEX AND DOT HAVE BEEN CHANGED AS OF NOW \/
	}

	/**
	 * Creates a new <code>ActiveArc</code> given a <code>Key</code>, which
	 * matches the next source-side RHS, which is needed by this
	 * <code>ActiveArc</code> OR if such an arc has already been created, a
	 * backpointer is simply added; thus, this is the primary method responsible
	 * for ambiguity packing in the <code>ChartParser</code> along with the
	 * create*Arc methods in the <code>ActiveArcManager</code>. Typically, only
	 * <code>ActiveArcManager.extendArcs</code> should need to call this method.
	 * 
	 * @param key
	 *            A <code>Key</code>, whose source LHS should match the
	 *            source-side RHS constituent directly to the right of the dot
	 *            for this <code>ActiveArc</code>.
	 * @return a new <code>ActiveArc</code> with its dot moved one position to
	 *         the right and with a backpointer to the given <code>Key</code>
	 *         added.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArc<R, T> extend(Key<R, T> key) {

		assert this.getEndIndex() == key.getStartIndex() : "Discontiguous arc extension.";

		ActiveArc<R, T> result =
				(ActiveArc<R, T>) new ActiveArc(this.getStartIndex(), key.getEndIndex(),
						this.getDot() + 1, firstRule, this.rules, this.parentArcs, this);
		result.addBackpointer(this.getDot(), key);
		return result;
	}

	/**
	 * Adds a backpointer to an existing <code>Key</code> this
	 * <code>ActiveArc</code> for the specified source-side RHS consituent so
	 * that we can later retrace these backpointers to create a parse tree or
	 * parse forest (hypergraph).
	 * 
	 * @param index
	 *            The zero-based index of the source-side RHS constituent that
	 *            this backpointer references.
	 * @param key
	 *            The <code>Key</code> that will be added as a backpointer.
	 */
	public void addBackpointer(int index, Key<R, T> key) {

		assert index < getDot() : "Index must fall to the left of the dot for this ActiveArc: "
				+ index + " should be less than " + getDot();
		assert key.getStartIndex() >= this.getStartIndex()
				&& key.getStartIndex() <= this.getEndIndex() : "Backpointer key's start index is outside the bounds of this arc";
		assert key.getEndIndex() >= this.getStartIndex() && key.getEndIndex() <= this.getEndIndex() : "Backpointer key's end index is outside the bounds of this arc";
		assert backpointersForCurrentDot.contains(key) == false : "arc " + this.toString()
				+ " already contains this backpointer: " + key.toString() + " -- duplicate: "
				+ backpointersForCurrentDot.get(backpointersForCurrentDot.indexOf(key)).toString();

		backpointersForCurrentDot.add(key);
		totalElements++;
		if (totalElements % 1000 == 0) {
			log.fine("Average backpointer list size: " + (double) totalElements
					/ (double) totalLists);
		}
	}

	/**
	 * Adds a GrammarRule to this ActiveArc; all such grammar rules must have
	 * the same requirements according to rule.getPackingString(), but may have
	 * unique target sides.
	 * 
	 * @param rule
	 *            the rule to be added
	 */
	public void addRule(R rule) {
		rules.add(rule);
	}

	/**
	 * Gets the list of backpointers associated with a particular RHS element.
	 * The indices in the <code>List</code> correspond to the order in which
	 * backpointers were added; the lengths of the <code>List</code>s for
	 * different values of sourceRhsIndex will often NOT be the same.
	 * 
	 * @param sourceRhsIndex
	 *            the source-side RHS index for which the backpointer list is
	 *            desired
	 * @return a list of backpointers to Keys
	 */
	public List<Key<R, T>> getBackpointers(int sourceRhsIndex) {
		assert sourceRhsIndex < getDot() : "sourceRhsIndex > dot for this arc";

		if (sourceRhsIndex == getDot() - 1) {
			return backpointersForCurrentDot;
		} else {
			return parentArcs[sourceRhsIndex].backpointersForCurrentDot;
		}
	}

	/**
	 * Gets the zero-based source-side input index where this
	 * <code>ActiveArc</code> begins.
	 * 
	 * @return the start index
	 */
	public int getStartIndex() {
		int startIndex = data & 0x000000ff;
		return startIndex;
	}

	/**
	 * Gets the zero-based source-side input index where this
	 * <code>ActiveArc</code> currently ends, given the dot position.
	 * 
	 * @return the end index
	 */
	public int getEndIndex() {
		int endIndex = data & 0x0000ff00;
		endIndex >>= 8;
		return endIndex;
	}

	/**
	 * Gets the symbol that is needed next to make further progress toward
	 * completing this <code>ActiveArc</code>.
	 * 
	 * @return NULL if no more constituents are needed; the constituent
	 *         (terminal or non-terminal) directly to the right of the "dot"
	 *         otherwise.
	 */
	public T getNeededSymbol() {
		if (getDot() >= getRhs().length) {
			return null;
		} else {
			T needed = getRhs()[getDot()];
			return needed;
		}
	}

	/**
	 * Gets the <code>GrammarRules</code> that called for the creation of this
	 * <code>ActiveArc</code>.
	 * 
	 * @return a list of grammar rules
	 */
	public Set<R> getRules() {
		return Collections.unmodifiableSet(rules);
	}

	/**
	 * Gets the source-side LHS, which is shared by all rules that formed this
	 * arc.
	 * 
	 * @return the source-side LHS
	 */
	public T getLhs() {
		return firstRule.getLhs();
	}

	/**
	 * Gets the source-side RHS, which is shared by all rules that formed this
	 * arc.
	 * 
	 * @return the source-side RHS
	 */
	public T[] getRhs() {
		return firstRule.getRhs();
	}

	/**
	 * Gets the number of source-side RHS consituents that have been completed
	 * in this <code>ActiveArc</code>.
	 * 
	 * @return the dot position
	 */
	public int getDot() {
		int dot = data & 0xffff0000;
		dot >>= 16;
		return dot;
	}

	/**
	 * Determines if the given Key (meaning any of the rules it contains)
	 * satisfies the rule constraints for <i>at least</i> one of the rules in
	 * this arc.
	 * 
	 * @param sourceRhsIndex
	 *            The source-side RHS index for which constraints will be
	 *            checked.
	 * @param key
	 *            The proposed key that we might use to extend this are at the
	 *            given RHS position.
	 * @return True if the key satisfies the constraints of at least of of the
	 *         rules in this arc; false otherwise.
	 */
	public boolean areConstraintsSatisfied(int sourceRhsIndex, Key<R, T> key) {
		for (R arcRule : rules) {
			assert Arrays.equals(getRhs(), arcRule.getRhs()) : "Incompatible RHS's detected in active arc.";

			for (ActiveArc<R, T> arc : key.getActiveArcs()) {
				for (R keyRule : arc.getRules()) {
					if (arcRule.areConstraintsSatisfied(sourceRhsIndex, keyRule)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Assuming that an existing arc and a candidate arc have the same start and
	 * end points in the input sequence and both arcs have the same constituent
	 * to the right of their dots, gets any further items that must be matched
	 * by the candidate arc for that arc to be packed into this existing arc.
	 * (Usually the source-side LHS concatenated with the source-side RHS).
	 * 
	 * @return the items that must be matched as a sequence of tokens
	 */
	public TokenSequence<T> getArcPackingString() {
		return firstRule.getArcPackingString();
	}

	public T getKeyPackingString() {
		return firstRule.getKeyPackingString();
	}

	/**
	 * A string representation of this <code>ActiveArc</code> that includes the
	 * source-side constituents that have been completed, a dot, and the
	 * source-side constituents that are still waiting to be completed along
	 * with the zero-based start and end indices for this arc.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getLhs().getWord());

		T[] rhs = getRhs();
		if (rhs.length > 0) {
			builder.append(" -> ");
		}

		for (int i = 0; i < rhs.length; i++) {
			if (i == getDot()) {
				builder.append("* ");
			}
			builder.append(rhs[i].getWord() + " ");
		}
		if (rhs.length == getDot()) {
			builder.append("* ");
		}
		return "[" + builder.toString() + "] (" + getStartIndex() + "," + getEndIndex() + ")";
	}
}
