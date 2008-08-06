package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.util.ArrayList;

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

	/**
	 * The initial list size for the backpointer lists. Increasing this value
	 * will take up more memory initially, but will reduce the amount of time
	 * spent growing the backpointer vectors if the number of backpointers
	 * created during parsing typically exceeds this number.
	 */
	public static final int DEFAULT_BACKPOINTER_LIST_SIZE = 5;

	private final int startIndex;
	private final int endIndex;
	private final R rule;
	private final ArrayList<Key<R, T>>[] backpointers;

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
	 * @param rule
	 *            The <code>GrammarRule</code> that called for the creation of
	 *            this <code>ActiveArc</code> given the current state of the
	 *            <code>Chart</code>.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArc(int startIndex, int endIndex, int dot, R rule) {
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.rule = rule;
		this.backpointers = (ArrayList<Key<R, T>>[]) new ArrayList[dot];
	}

	/**
	 * Creates a new <code>ActiveArc</code> given a <code>Key</code>, which
	 * matches the next source-side RHS, which is needed by this
	 * <code>ActiveArc</code>. Typically, only
	 * <code>ActiveArcManager.extendArcs</code> should need to call this method.
	 * 
	 * @param key
	 *            A <code>Key</code>, whose source LHS should match the
	 *            source-side RHS constituent directly to the right of the dot
	 *            for this <code>ActiveArc</code>.
	 * @return A new <code>ActiveArc</code> with its dot moved one position to
	 *         the right and with a backpointer to the given <code>Key</code>
	 *         added.
	 */
	@SuppressWarnings("unchecked")
	public ActiveArc<R, T> extend(Key<R, T> key) {

		assert this.getEndIndex() == key.getStartIndex() : "Discontiguous arc extension.";

		ActiveArc<R, T> extendedArc =
				(ActiveArc<R, T>) new ActiveArc(this.getStartIndex(), key.getEndIndex(),
						this.getDot() + 1, this.getRule());
		System.arraycopy(this.backpointers, 0, extendedArc.backpointers, 0,
				this.backpointers.length);
		extendedArc.addBackpointer(this.getDot(), key);
		return extendedArc;
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

		assert index < getDot() : "Index must fall to the left of the dot for this ActiveArc: " + index
				+ " !< " + getDot();

		if (backpointers[index] == null) {
			backpointers[index] = new ArrayList<Key<R, T>>(DEFAULT_BACKPOINTER_LIST_SIZE);
		}
		backpointers[index].add(key);
	}

	/**
	 * Gets an array of lists of the backpointers for this
	 * <code>ActiveArc</code>. The array has length <code>getDot()</code> such
	 * that the array indices correspond to the source-side RHS constituents,
	 * which the backpointers refer to. The indices in each <code>List</code>
	 * correspond to the order in which backpointers were added; the lengths of
	 * the <code>List</code>s in this array will often NOT be the same.
	 * 
	 * @return
	 */
	public ArrayList<Key<R, T>>[] getBackpointers() {
		return backpointers;
	}

	/**
	 * Gets the zero-based source-side input index where this
	 * <code>ActiveArc</code> begins.
	 * 
	 * @return the start index
	 */
	public int getStartIndex() {
		return startIndex;
	}

	/**
	 * Gets the zero-based source-side input index where this
	 * <code>ActiveArc</code> currently ends, given the dot position.
	 * 
	 * @return the end index
	 */
	public int getEndIndex() {
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
		if (getDot() >= rule.getRhs().length) {
			return null;
		} else {
			T needed = rule.getRhs()[getDot()];
			return needed;
		}
	}

	/**
	 * Gets the <code>GrammarRule</code> that called for the creation of this
	 * <code>ActiveArc</code>.
	 * 
	 * @return a grammar rule
	 */
	public R getRule() {
		return rule;
	}

	/**
	 * Gets the number of source-side RHS consituents that have been completed
	 * in this <code>ActiveArc</code>.
	 * 
	 * @return the dot position
	 */
	public int getDot() {
		return backpointers.length;
	}

	/**
	 * A string representation of this <code>ActiveArc</code> that includes the
	 * source-side constituents that have been completed, a dot, and the
	 * source-side constituents that are still waiting to be completed along
	 * with the zero-based start and end indices for this arc; Tokens will have
	 * only their ID shown.
	 * 
	 * @return a string representation of this object
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(rule.getLhs().getId());

		T[] rhs = rule.getRhs();
		if (rhs.length > 0) {
			builder.append(" -> ");
		}

		for (int i = 0; i < rhs.length; i++) {
			if (i == getDot()) {
				builder.append("* ");
			}
			builder.append(rhs[i].getId() + " ");
		}
		if (rhs.length == getDot()) {
			builder.append("* ");
		}
		return "[" + builder.toString() + "] (" + startIndex + "," + endIndex + ")";
	}

	/**
	 * A string representation of this <code>ActiveArc</code> that includes the
	 * source-side constituents that have been completed, a dot, and the
	 * source-side constituents that are still waiting to be completed along
	 * with the zero-based start and end indices for this arc. Tokens will be
	 * resolved to their string form via the provided <code>TokenFactory</code>.
	 * 
	 * @return a string representation of this object
	 */
	public String toString(TokenFactory<T> symbolFactory) {
		StringBuilder builder = new StringBuilder();
		builder.append(symbolFactory.getTokenAsString(rule.getLhs()));

		T[] rhs = rule.getRhs();
		if (rhs.length > 0) {
			builder.append(" -> ");
		}

		for (int i = 0; i < rhs.length; i++) {
			if (i == getDot()) {
				builder.append("* ");
			}
			builder.append(symbolFactory.getTokenAsString(rhs[i]) + " ");
		}
		if (rhs.length == getDot()) {
			builder.append("* ");
		}
		return "[" + builder.toString() + "] (" + startIndex + "," + endIndex + ")";
	}
}
