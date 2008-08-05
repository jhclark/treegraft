package info.jonclark.treegraft.lm;

import info.jonclark.treegraft.core.tokens.Token;

import java.io.IOException;
import java.io.InputStream;

public interface LanguageModel {
	public void load(InputStream inputStream) throws IOException;
	public double score(Token[] tokens);
}
