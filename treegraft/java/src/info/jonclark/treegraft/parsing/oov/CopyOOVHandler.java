package info.jonclark.treegraft.parsing.oov;

import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.core.featureimpl.RuleScore;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CopyOOVHandler<T extends Token> implements OutOfVocabularyHandler<SyncCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final RuleScore oovRuleScore;
	private final T[] oovRuleLhsList;
	private final int[] alignment;
	private int oovCounter = 0;

	public CopyOOVHandler(SmartProperties props, TokenFactory<T> tokenFactory) {

		this.tokenFactory = tokenFactory;
		this.alignment = new int[] { -1 };

		double sgt = props.getPropertyFloat("grammar.oovHandler.oovRuleSgtLogProb");
		double tgs = props.getPropertyFloat("grammar.oovHandler.oovRuleTgsLogProb");
		this.oovRuleScore = new RuleScore(sgt, tgs);
		this.oovRuleLhsList =
				tokenFactory.makeTokens(
						props.getPropertyStringArray("grammar.oovHandler.oovRuleLhsList"), true);
	}

	public List<SyncCFGRule<T>> generateOOVRules(T sourceOovTerminal,
			List<T> sourceInputBeforeOovTerminal) throws RuleException {

		ArrayList<SyncCFGRule<T>> rules = new ArrayList<SyncCFGRule<T>>(1);

		T[] copyRhs = tokenFactory.newTokenArray(1);
		copyRhs[0] = sourceOovTerminal;

		for (T oovRuleLhs : oovRuleLhsList) {
			SyncCFGRule<T> rule =
					new SyncCFGRule<T>(oovRuleLhs, copyRhs, oovRuleLhs, copyRhs,
							"OOV" + oovCounter, alignment, oovRuleScore, null, "none", 0,
							tokenFactory);
			rules.add(rule);
			oovCounter++;
		}
		return rules;
	}

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> sourceVocab) {
		return sourceVocab;
	}

}
