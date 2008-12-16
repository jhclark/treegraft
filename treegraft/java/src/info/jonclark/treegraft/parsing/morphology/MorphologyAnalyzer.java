package info.jonclark.treegraft.parsing.morphology;

import info.jonclark.treegraft.core.tokens.Token;

import java.util.HashSet;

public interface MorphologyAnalyzer<T extends Token> {
	
	public HashSet<T> getAdditionalSourceVocabulary(HashSet<T> inputVocabulary);
}
