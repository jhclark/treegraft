package info.jonclark.treegraft.parsing.grammar;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface GrammarPruner<R extends GrammarRule<T>, T extends Token> {
	public void pruneGrammar(Grammar<R,T> grammar);
}
