package info.jonclark.treegraft.core.recombination;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

public class NullRecombiner<R extends GrammarRule<T>, T extends Token> implements
		ParseRecombiner<R, T> {

	public void recombine(List<Parse<T>> parses, Scorer<R, T> scorer) {
		// do nothing
	}
}
