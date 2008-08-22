package info.jonclark.treegraft.core.mergingX;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.List;

public interface ParsePruner<R extends GrammarRule<T>, T extends Token> {
	
	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<Parse<T>>[] parses,
			Scorer<R, T> scorer, Transducer<R, T> transducer);

	public void combineCrossProductOfHypotheses(Scorer<R, T> scorer, Beam<DecoderHypothesis<T>> beam1,
			Beam<DecoderHypothesis<T>> beam2, Beam<DecoderHypothesis<T>> outputBeam);
}
