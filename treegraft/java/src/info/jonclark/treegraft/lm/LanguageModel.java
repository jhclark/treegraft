package info.jonclark.treegraft.lm;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.io.IOException;
import java.io.InputStream;

public interface LanguageModel<T extends Token> {
	public void load(InputStream inputStream) throws IOException;
	public double score(TokenSequence<T> tokens);
}
