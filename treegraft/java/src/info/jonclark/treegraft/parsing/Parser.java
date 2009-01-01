package info.jonclark.treegraft.parsing;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;

public interface Parser<R extends GrammarRule<T>, T extends Token> extends Plugin<R,T> {
	public Chart<R, T> parse(T[] input) throws RuleException;
}
