package info.jonclark.treegraft.core.forestunpacking.pruning;

import info.jonclark.treegraft.core.forestunpacking.parses.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.scoring.ParseScorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.transduction.Transducer;

import java.util.List;

public interface ParsePruner<R extends GrammarRule<T>, T extends Token> {
	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			ParseScorer<R, T> scorer, Transducer<R, T> transducer);
}
