package info.jonclark.treegraft.parsing.oov;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;

import java.util.HashSet;
import java.util.List;

public interface OutOfVocabularyHandler<R extends GrammarRule<T>, T extends Token> {
	
	public List<R> generateOOVRules(T sourceOovTerminal, List<T> sourceInputBeforeOovTerminal)
			throws RuleException;

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> sourceVocab);
}
