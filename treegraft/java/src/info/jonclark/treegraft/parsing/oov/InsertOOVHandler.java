package info.jonclark.treegraft.parsing.oov;

import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;

import java.util.ArrayList;
import java.util.List;

public class InsertOOVHandler<T extends Token> implements OutOfVocabularyHandler<SyncCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final double oovRuleLogProb;
	private final T[] oovRuleLhsList;
	private final int[] alignment;
	private final T[] phraseToInsert;
	private int oovCounter = 0;

	public InsertOOVHandler(SmartProperties props, TokenFactory<T> tokenFactory) {

		this.tokenFactory = tokenFactory;

		this.alignment = new int[] { -1 };

		this.oovRuleLogProb = props.getPropertyFloat("grammar.oovHandler.oovRuleLogProb");
		this.oovRuleLhsList =
				tokenFactory.makeTokens(
						props.getPropertyStringArray("grammar.oovHandler.oovRuleLhsList"), true);
		this.phraseToInsert =
				tokenFactory.makeTokens(
						props.getPropertyStringArray("grammar.oovHandler.phraseToInsert"), true);
	}

	public List<SyncCFGRule<T>> generateOOVRules(T sourceOovTerminal,
			List<T> sourceInputBeforeOovTerminal) throws RuleException {

		ArrayList<SyncCFGRule<T>> rules = new ArrayList<SyncCFGRule<T>>(1);

		T[] copyRhs = tokenFactory.newTokenArray(1);
		copyRhs[0] = sourceOovTerminal;

		for (T oovRuleLhs : oovRuleLhsList) {
			SyncCFGRule<T> rule =
					new SyncCFGRule<T>(oovRuleLhs, copyRhs, oovRuleLhs, phraseToInsert, "OOV"
							+ oovCounter, alignment, oovRuleLogProb, null, "none", 0, tokenFactory);
			rules.add(rule);
			oovCounter++;
		}
		return rules;
	}

}
