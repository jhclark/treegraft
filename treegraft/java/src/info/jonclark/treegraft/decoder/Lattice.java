package info.jonclark.treegraft.decoder;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Lattice<R extends GrammarRule<T>, T extends Token> {

	private final List<T> sourceInputTokens;
	private final List<PartialParse<T>>[][] lattice;
	// private final List<PartialParse<T>> result = new
	// ArrayList<PartialParse<T>>();
	private static final Logger log = LogUtils.getLogger();

	// TODO: Change this to a Lattice datatype
	public Lattice(Chart<R, T> chart, ForestUnpacker<R, T> unpacker, ParseFactory<R, T> parseFactory) {

		sourceInputTokens = chart.getSourceInputTokens();
		lattice = createEmptyTransferBeams(chart.getInputLength());

		// for each source key, keep a beam of the best hypotheses that it can
		// create each backpointer from higher-level keys will have access to
		// only the partial parses in that beam
		for (Key<R, T> key : chart.getKeys()) {
			lattice[key.getStartIndex()][key.getEndIndex()] =
					unpacker.getPartialParses(key, chart.getSourceInputTokens(), parseFactory);
		}
	}

	private List<PartialParse<T>>[][] createEmptyTransferBeams(final int N) {
		List<PartialParse<T>>[][] beams = new List[N + 1][N + 1];
		for (int i = 0; i < beams.length; i++) {
			for (int j = 0; j < beams[i].length; j++) {
				beams[i][j] = new ArrayList<PartialParse<T>>();
			}
		}
		return beams;
	}
	
	public List<PartialParse<T>> getPartialParses(int i, int j) {
		return lattice[i][j];
	}

	public int getInputLength() {
		return lattice.length - 1;
	}
	
	public List<T> getSourceInputTokens() {
		return sourceInputTokens;
	}
}
