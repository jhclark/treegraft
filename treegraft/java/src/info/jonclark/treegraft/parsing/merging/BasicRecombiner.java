package info.jonclark.treegraft.parsing.merging;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BasicRecombiner<R extends GrammarRule<T>, T extends Token> implements Recombiner<R, T> {

	private ConcurrentMap<List<T>, PartialParse<T>> concurrentHashMap;
	private Scorer<R, T> scorer;

	public BasicRecombiner(int maxParseCount, Scorer<R, T> scorer) {
		concurrentHashMap = new ConcurrentHashMap<List<T>, PartialParse<T>>(maxParseCount);
		this.scorer = scorer;
	}

	// returns true if recombination was performed
	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.parsing.merging.Recombiner#recombine(info.jonclark.treegraft.parsing.parses.PartialParse)
	 */
	public boolean recombine(PartialParse<T> expandedParse) {

		PartialParse<T> previousParseWithSameYield =
				concurrentHashMap.putIfAbsent(expandedParse.getTargetTokens(), expandedParse);

		if (previousParseWithSameYield != null) {
			FeatureScores recombinedScores =
					scorer.recombineParses(previousParseWithSameYield, expandedParse);
			previousParseWithSameYield.setCurrentScore(recombinedScores);
			previousParseWithSameYield.addRecombinedParse(expandedParse);
			return true;
		} else {
			return false;
		}
	}
}
