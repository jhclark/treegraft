package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

public interface Decoder<R extends GrammarRule<T>, T extends Token> {
	public List<DecoderHypothesis<T>> getKBest(Chart<R, T> chart, ForestUnpacker<R, T> unpacker);
}
