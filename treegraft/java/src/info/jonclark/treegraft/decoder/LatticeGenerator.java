package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.core.forestunpacking.parses.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.List;

public class LatticeGenerator<R extends GrammarRule<T>, T extends Token> {

	public List<LatticeArc<T>> getLattice(Chart<R, T> chart, ForestUnpacker<R, T> unpacker) {
		List<LatticeArc<T>> result = new ArrayList<LatticeArc<T>>();

		// TODO: cache the results from the ForestUnpacker in a dynamic
		// programming fashion

		for (Key<R, T> key : chart.getKeys()) {
			List<Parse<T>> partialParses = unpacker.getPartialParses(key);
			for (Parse<T> parse : partialParses) {
				result.add(new LatticeArc<T>(key.getStartIndex(), key.getEndIndex(), parse));
			}
		}

		return result;
	}
}
