package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.net.InetSocketAddress;
import java.util.HashSet;

public class RemoteMorphologyGenerator<T extends Token> implements MorphologyGenerator<T> {

	public RemoteMorphologyGenerator(InetSocketAddress host) {

	}

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> grammarVocabulary) {
		throw new Error("Unimplemented.");
	}
}
