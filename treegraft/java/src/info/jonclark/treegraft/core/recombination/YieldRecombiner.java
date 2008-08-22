package info.jonclark.treegraft.core.recombination;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.HashMap;
import java.util.List;

// TODO: Move these merging details to the ParsePruner/Merger
public class YieldRecombiner<R extends GrammarRule<T>, T extends Token> implements
		ParseRecombiner<R, T> {

	private final TokenFactory<T> tokenFactory;

	public YieldRecombiner(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	public void recombine(List<Parse<T>> parses, Scorer<R, T> scorer) {
		// combine parses with the same yield

		HashMap<TokenSequence<T>, Parse<T>> uniqueParses =
				new HashMap<TokenSequence<T>, Parse<T>>();

		for (int i = 0; i < parses.size(); i++) {
			Parse<T> currentParse = parses.get(i);
			Parse<T> previousParseWithSameYield = uniqueParses.get(currentParse);
			if (previousParseWithSameYield != null) {

				// duplicate detected, sum scores
				FeatureScores combinedScore =
						scorer.recombineParses(previousParseWithSameYield, currentParse);
				previousParseWithSameYield.setCurrentScore(combinedScore);
				previousParseWithSameYield.addRecombinedParse(currentParse);

				parses.remove(i);
				i--;
			} else {
				TokenSequence<T> seq =
						tokenFactory.makeTokenSequence(currentParse.getTargetTokens());
				uniqueParses.put(seq, currentParse);
			}
		}
	}

	public void recombine(Beam<DecoderHypothesis<T>> beam1, Beam<DecoderHypothesis<T>> beam2,
			Beam<DecoderHypothesis<T>> outputBeam) {

	}
}
