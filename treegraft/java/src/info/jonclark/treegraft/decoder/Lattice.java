package info.jonclark.treegraft.decoder;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class Lattice<R extends GrammarRule<T>, T extends Token> {

	private final List<LatticeArc<T>> result = new ArrayList<LatticeArc<T>>();
	private static final Logger log = LogUtils.getLogger();

	// TODO: Change this to a Lattice datatype
	public Lattice(Chart<R, T> chart, ForestUnpacker<R, T> unpacker) {

		for (Key<R, T> key : chart.getKeys()) {
			List<Parse<T>> partialParses = unpacker.getPartialParses(key);
			for (Parse<T> parse : partialParses) {
				result.add(new LatticeArc<T>(key.getStartIndex(), key.getEndIndex(), parse));
			}
		}

		// sort by start index, end index, then score
		Collections.sort(result, new Comparator<LatticeArc<T>>() {
			public int compare(LatticeArc<T> o1, LatticeArc<T> o2) {
				if (o1.getStartIndex() < o2.getStartIndex()) {
					return -1;
				} else if (o1.getStartIndex() > o2.getStartIndex()) {
					return 1;
				} else {
					if (o1.getEndIndex() < o2.getEndIndex()) {
						return -1;
					} else if (o1.getEndIndex() > o2.getEndIndex()) {
						return 1;
					} else {
						if (o1.getParse().getLogProb() > o2.getParse().getLogProb()) {
							return -1;
						} else if (o1.getParse().getLogProb() < o2.getParse().getLogProb()) {
							return 1;
						} else {
							return 0;
						}
					}
				}
			}
		});
	}

	// TODO: Refactor into a lattice formatter class
	public String toString(TreeFormatter<T> treeFormatter) {

		StringBuilder builder = new StringBuilder();
		for (LatticeArc<T> arc : result) {
			builder.append(arc.toString(treeFormatter) + "\n");
		}

		builder.append("\n");
		return builder.toString();
	}
}
