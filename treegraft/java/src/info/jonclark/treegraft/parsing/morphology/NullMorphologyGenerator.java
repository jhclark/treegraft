package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.util.HashSet;

public class NullMorphologyGenerator<T extends Token> implements MorphologyGenerator<T> {

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> grammarVocabulary) {
		return new HashSet<T>(0);
	}

}
