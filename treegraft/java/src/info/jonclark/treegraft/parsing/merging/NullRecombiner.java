package info.jonclark.treegraft.parsing.merging;

import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public class NullRecombiner<R extends GrammarRule<T>, T extends Token> implements Recombiner<R, T> {

	public NullRecombiner(int maxParseCount, Scorer<R, T> scorer) {

	}

	// returns true if recombination was performed
	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.parsing.merging.Recombiner#recombine(info.jonclark.treegraft.parsing.parses.PartialParse)
	 */
	public boolean recombine(PartialParse<T> expandedParse) {
		return false;
	}
}
