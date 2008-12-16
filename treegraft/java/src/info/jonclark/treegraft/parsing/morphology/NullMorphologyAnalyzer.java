package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.util.HashSet;

public class NullMorphologyAnalyzer<T extends Token> implements MorphologyAnalyzer<T> {

	public HashSet<T> getAdditionalSourceVocabulary(HashSet<T> grammarVocabulary) {
		return new HashSet<T>(0);
	}

}
