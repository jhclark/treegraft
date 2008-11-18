package info.jonclark.treegraft.core.lm;

import info.jonclark.stat.TaskListener;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;

public interface LanguageModelLoader<T extends Token> {
	
	/**
	 * Responsibility to populate n-gram table and set the OOV prob
	 * 
	 * @param lm
	 * @param tokenFactory
	 * @param stream
	 * @param encoding
	 * @param targetVocabulary
	 * @param oovLogProb
	 * @param task
	 * @throws IOException
	 */
	public void loadLM(LanguageModel<T> lm, TokenFactory<T> tokenFactory, InputStream stream,
			String encoding, HashSet<T> targetVocabulary, TaskListener task) throws IOException;
}
