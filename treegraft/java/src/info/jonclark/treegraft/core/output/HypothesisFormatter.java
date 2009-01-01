package info.jonclark.treegraft.core.output;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.io.PrintWriter;

public interface HypothesisFormatter<R extends GrammarRule<T>, T extends Token> extends
		Plugin<R, T> {
	
	public void formatHypothesis(Result<R, T> hyp, PrintWriter out);
}
