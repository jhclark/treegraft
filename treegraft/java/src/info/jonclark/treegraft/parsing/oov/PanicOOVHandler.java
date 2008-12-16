package info.jonclark.treegraft.parsing.oov;

import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;

import java.util.HashSet;
import java.util.List;

public class PanicOOVHandler<R extends GrammarRule<T>, T extends Token> implements
		OutOfVocabularyHandler<R, T> {

	private final TokenFactory<T> tokenFactory;

	public PanicOOVHandler(SmartProperties props, TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	public List<R> generateOOVRules(T sourceOovTerminal, List<T> sourceInputBeforeOovTerminal)
			throws RuleException {

		throw new RuntimeException("Encountered unexpected OOV word: "
				+ tokenFactory.getTokenAsString(sourceOovTerminal));
	}

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> sourceVocab) {
		return new HashSet<T>(0);
	}

}
