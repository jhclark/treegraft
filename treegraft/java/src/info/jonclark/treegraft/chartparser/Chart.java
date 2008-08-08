package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.formatting.forest.ParseForestFormatter;
import info.jonclark.treegraft.core.formatting.parses.Parse;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A packed forest of <code>Keys</code> that represent the search space of valid
 * parses (and partial parses) for the input sequence that generated the
 * <code>Chart</code>.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class Chart<R extends GrammarRule<T>, T extends Token> {

	/**
	 * A wrapper for ambiguity lists that handle the packing of arcs. See also
	 * ActiveArcManager.PackedArc
	 */
	private class KeyGroup {
		// TODO: exchange array list for a scored beam
		public ArrayList<Key<R, T>> list =
				new ArrayList<Key<R, T>>(ActiveArcManager.DEFAULT_PACKING_SIZE);
	}

	/**
	 * The initial number of keys that space should be allocated for.
	 */
	public static final int DEFAULT_CHART_SIZE = 10000;

	private final HashMap<Key<R, T>, KeyGroup> chart =
			new HashMap<Key<R, T>, KeyGroup>(DEFAULT_CHART_SIZE);
	private final ArrayList<Key<R, T>> parses = new ArrayList<Key<R, T>>();
	private final ArrayList<Key<R, T>> keys = new ArrayList<Key<R, T>>(DEFAULT_CHART_SIZE);

	/**
	 * Adds a <code>Key</code> to this <code>Chart</code> after it has been
	 * removed from the <code>Agenda</code> and its implications have been
	 * processed. This method should typically only be called by the
	 * <code>ChartParser</code>.
	 * 
	 * @param key
	 *            the key to be added
	 */
	public void addKey(Key<R, T> key) {
		// do ambiguity packing by virtue of id choice and hashing,
		// this allows us to store multiple ambiguities in the array list which
		// is the map value (NOTE: ambiguity packing is also done in the
		// ActiveArcManager)
		append(chart, key, key);
		keys.add(key);
	}

	private void append(Map<Key<R, T>, KeyGroup> map, Key<R, T> key, Key<R, T> valueToAppend) {

		KeyGroup packedKey = map.get(key);
		if (packedKey == null) {
			packedKey = new KeyGroup();
			map.put(key, packedKey);
		}
		packedKey.list.add(valueToAppend);
	}

	/**
	 * Adds a <code>Key</code> (that has already been added to this
	 * <code>Chart</code> using <code>addKey()</code>) when it represents a
	 * complete parse. This method should typically only be called by the
	 * <code>ChartParser</code>.
	 * 
	 * @param key
	 *            the key that represents a complete parse
	 */
	public void addParse(Key<R, T> key) {
		assert chart.containsKey(key) : "Key for parse not in chart.";
		parses.add(key);
	}

	/**
	 * Check if the input is grammatical according to the given Grammar.
	 * 
	 * @return True if one or more complete parses was found; false otherwise.
	 */
	public boolean isInputGrammatical() {
		return !parses.isEmpty();
	}

	/**
	 * Gets the set of all <code>Keys</code> that are contained in this
	 * <code>Chart</code> in the order in which they were added.
	 * 
	 * @return a set of keys
	 */
	public List<Key<R, T>> getKeys() {
		return keys;
	}

	/**
	 * Gets the parses from this <code>Chart</code> that are grammatical
	 * according to the Grammar. These format of these parses (including whether
	 * a source tree, target tree, or target string is produced) is determined
	 * by the <code>ParseFormatter</code>.
	 * 
	 * @param formatter
	 *            the <code>ParseFormatter</code> that will determine the format
	 *            of the returned parses
	 * @return an array of formatted grammatical parses
	 */
	@SuppressWarnings("unchecked")
	public Parse<R, T>[] getGrammaticalParses(ParseFormatter<R, T> formatter) {
		ArrayList<Parse<R, T>> grammaticalParses = new ArrayList<Parse<R, T>>();
		for (final Key<R, T> k : parses) {
			for (final Parse<R, T> p : k.getPartialParses(formatter)) {
				grammaticalParses.add(p);
			}
		}
		return (Parse<R, T>[]) grammaticalParses.toArray(new Parse[grammaticalParses.size()]);
	}

	/**
	 * Get the parse forest (hypergraph) contained in this <code>Chart</code>
	 * including both grammatical and ungrammatical parses.
	 * 
	 * @param <F>
	 *            The type of the parse Forest that will be returned
	 * @param formatter
	 *            a <code>ParseForestFormatter</code> that will determine how
	 *            the returned parse forest is constructed including whether it
	 *            is a source-side or target-side forest.
	 * @return A parse forest derived from this <code>Chart</code>.
	 */
	public <F> F getParseForest(ParseForestFormatter<R, T, F> formatter) {

		for (Key<R, T> key : keys) {
			if (key.isTerminal()) {
//				formatter.addTerminal(key);
			} else {
				formatter.addNonterminal(key);
			}
		}

		return formatter.getParseForest();
	}
}
