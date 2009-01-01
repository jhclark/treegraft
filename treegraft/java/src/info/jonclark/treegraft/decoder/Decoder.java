package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

public interface Decoder<R extends GrammarRule<T>, T extends Token> extends Plugin<R,T> {
	public List<PartialParse<T>> getKBest(Lattice<R,T> targetLattice, ParseFactory<R,T> parseFactory);
}
