package info.jonclark.treegraft.parsing.forestunpacking;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface ForestProcessor<R extends GrammarRule<T>, T extends Token> extends Plugin<R, T> {

}
