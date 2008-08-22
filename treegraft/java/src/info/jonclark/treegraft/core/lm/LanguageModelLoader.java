package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

public interface LanguageModelLoader<T extends Token> {
	public void loadLM(LanguageModel<T> lm, TokenFactory<T> tokenFactory, InputStream stream,
			String encoding, HashSet<T> targetVocabulary, double oovLogProb) throws IOException;
}
