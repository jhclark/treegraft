package info.jonclark.treegraft.core.output;

import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.io.PrintWriter;

public interface HypothesisFormatter<R extends GrammarRule<T>, T extends Token> {
	public void formatHypothesis(Result<R,T> hyp, PrintWriter out);
}
