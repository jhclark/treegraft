package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.formatting.ParseForestFormatter;
import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A packed forest of Keys that represent the search space of valid parses (and
 * partial parses) for the input sequence that generated the Chart.
 * 
 * @author jon
 * @param <R>
 * @param <T>
 */
public class Chart<R extends GrammarRule<T>, T extends Token> {

	private class PackedKey {
		// TODO: exchange array list for a scored beam
		public ArrayList<Key<R, T>> list =
				new ArrayList<Key<R, T>>(ActiveArcManager.DEFAULT_PACKING_SIZE);
	}

	public static final int DEFAULT_CHART_SIZE = 1000;
	private final HashMap<Key<R, T>, PackedKey> chart =
			new HashMap<Key<R, T>, PackedKey>(DEFAULT_CHART_SIZE);
	private final ArrayList<Key<R, T>> parses = new ArrayList<Key<R, T>>();

	public void add(Key<R, T> key) {
		// do ambiguity packing by virtue of id choice and hashing,
		// this allows us to store multiple ambiguities in the array list which
		// is the map value
		append(chart, key, key);
	}

	public void append(Map<Key<R, T>, PackedKey> map, Key<R, T> key, Key<R, T> value) {

		PackedKey packedKey = map.get(key);
		if (packedKey == null) {
			packedKey = new PackedKey();
			map.put(key, packedKey);
		}
		packedKey.list.add(value);
	}

	public void addParse(Key<R, T> key) {
		assert chart.containsKey(key) : "Key for parse not in chart.";
		parses.add(key);
	}

	public boolean isInputGrammatical() {
		return !parses.isEmpty();
	}

	public Set<Key<R, T>> getKeys() {
		return chart.keySet();
	}

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
	
	public <F> F getParseForest(ParseForestFormatter<R, T, F> formatter) {
		return null;
	}
}
