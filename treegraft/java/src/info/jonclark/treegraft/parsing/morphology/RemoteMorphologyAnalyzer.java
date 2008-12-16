package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.net.InetSocketAddress;
import java.util.HashSet;

public class RemoteMorphologyAnalyzer<T extends Token> implements MorphologyAnalyzer<T> {

	public RemoteMorphologyAnalyzer(InetSocketAddress host) {

	}

	public HashSet<T> getAdditionalSourceVocabulary(HashSet<T> grammarVocabulary) {
		throw new Error("Unimplemented.");
	}
}
