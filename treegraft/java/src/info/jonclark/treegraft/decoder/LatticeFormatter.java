package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface LatticeFormatter<R extends GrammarRule<T>, T extends Token> extends Plugin<R, T> {
	public String format(Lattice<R, T> lattice);
}
