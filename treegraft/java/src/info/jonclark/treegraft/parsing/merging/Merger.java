package info.jonclark.treegraft.parsing.merging;

import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.List;

public interface Merger<R extends GrammarRule<T>, T extends Token> {

	public List<Parse<T>> combineCrossProductOfParses(R parentRule, List<T> sourceInputTokens,
			int sourceInputStartIndex, int sourceInputEndIndex, List<Parse<T>>[] parses,
			Scorer<R, T> scorer, Transducer<R, T> transducer, List<T> sourceTokens, ParseFactory<R,T> parseFactory);

	public void combineCrossProductOfHypotheses(Scorer<R, T> scorer,
			Beam<DecoderHypothesis<T>> beam1, Beam<DecoderHypothesis<T>> beam2,
			Beam<DecoderHypothesis<T>> outputBeam, List<T> sourceTokens);
}
