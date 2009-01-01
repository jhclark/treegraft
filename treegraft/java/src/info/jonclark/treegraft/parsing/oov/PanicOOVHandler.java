package info.jonclark.treegraft.parsing.oov;

import info.jonclark.lang.NullOptions;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;

import java.util.HashSet;
import java.util.List;

@OptionsTarget(NullOptions.class)
public class PanicOOVHandler<R extends GrammarRule<T>, T extends Token> implements
		OutOfVocabularyHandler<R, T> {

	public PanicOOVHandler(NullOptions opts, TreegraftConfig<R, T> config) {
	}

	public List<R> generateOOVRules(T sourceOovTerminal, List<T> sourceInputBeforeOovTerminal)
			throws RuleException {

		throw new RuntimeException("Encountered unexpected OOV word: "
				+ sourceOovTerminal.getWord());
	}

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> sourceVocab) {
		return new HashSet<T>(0);
	}

}
