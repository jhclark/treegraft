package info.jonclark.treegraft.foresreranking;

import java.util.List;

import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.search.Beam;

public class CubePruner<R extends GrammarRule<T>, T extends Token> implements ForestReranker<R, T> {

	public Beam<Parse<R, T>> getKBest(Chart<R, T> c, List<ScoringFunction<R, T>> scoringFunctions,
			int k) {
		// TODO Auto-generated method stub
		return null;
	}

}
