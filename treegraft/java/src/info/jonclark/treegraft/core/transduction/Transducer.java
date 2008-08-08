package info.jonclark.treegraft.core.transduction;

import java.util.HashMap;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

public abstract class Transducer<R extends GrammarRule<T>, T extends Token> {
	public abstract Vertex<R,T> transduce(Key<R,T> source);
}
