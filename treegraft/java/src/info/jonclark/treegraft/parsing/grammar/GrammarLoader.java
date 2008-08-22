package info.jonclark.treegraft.parsing.grammar;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public interface GrammarLoader<R extends GrammarRule<T>, T extends Token> {

	public void loadGrammar(Grammar<R, T> grammar, InputStream stream,
			String inputSourceName, String encoding) throws IOException, ParseException;
}
