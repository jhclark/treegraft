package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.util.HashSet;

public interface MorphologyGenerator<T extends Token> {
	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> inputVocabulary);
}
