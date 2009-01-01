package info.jonclark.treegraft.parsing.merging;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface Recombiner<R extends GrammarRule<T>, T extends Token> {

	// returns true if recombination was performed
	public boolean recombine(PartialParse<T> expandedParse);
}
