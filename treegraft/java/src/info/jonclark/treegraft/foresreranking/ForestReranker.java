package info.jonclark.treegraft.foresreranking;

import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.search.Beam;

import java.util.List;

public interface ForestReranker<R extends GrammarRule<T>, T extends Token> {
	public Beam<Parse<R, T>> getKBest(Chart<R, T> c, List<ScoringFunction<R, T>> scoringFunctions, int k);
}
