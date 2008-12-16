package info.jonclark.treegraft.parsing.grammar;

import info.jonclark.stat.TaskListener;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;

import java.io.IOException;
import java.io.InputStream;

public interface GrammarLoader<R extends GrammarRule<T>, T extends Token> {

	public void loadGrammar(Grammar<R, T> grammar, InputStream stream, String inputSourceName,
			String encoding, TaskListener task) throws IOException, RuleException;
}
