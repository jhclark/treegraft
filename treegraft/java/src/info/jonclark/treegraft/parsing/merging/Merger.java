package info.jonclark.treegraft.parsing.merging;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.List;

public interface Merger<R extends GrammarRule<T>, T extends Token> extends Plugin<R,T> {

	public List<PartialParse<T>> combineCrossProductOfChildParses(R parentRule,
			List<T> sourceInputTokens, int sourceInputStartIndex, int sourceInputEndIndex,
			Scorer<R, T> scorer, Transducer<R, T> transducer, ParseFactory<R, T> parseFactory,
			List<PartialParse<T>>... childParses);

	public void combineCrossProductOfHypotheses(Scorer<R, T> scorer,
			Beam<DecoderHypothesis<T>> beam1, Beam<DecoderHypothesis<T>> beam2,
			Beam<DecoderHypothesis<T>> outputBeam, List<T> sourceTokens);
}
