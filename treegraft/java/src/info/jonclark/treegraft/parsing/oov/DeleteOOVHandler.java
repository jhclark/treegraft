package info.jonclark.treegraft.parsing.oov;

import info.jonclark.lang.OptionsTarget;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.featureimpl.RuleScore;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.oov.CopyOOVHandler.CopyOrDeleteOOVHandlerOptions;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@OptionsTarget(CopyOOVHandler.CopyOrDeleteOOVHandlerOptions.class)
public class DeleteOOVHandler<T extends Token> implements OutOfVocabularyHandler<SyncCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final RuleScore oovRuleScore;
	private final T[] oovRuleLhsList;
	private final int[] alignment;
	private int oovCounter = 0;
	private final T[] blankRhs;

	public DeleteOOVHandler(CopyOrDeleteOOVHandlerOptions opts, TreegraftConfig<?, T> config) {

		this.tokenFactory = config.tokenFactory;
		this.alignment = new int[] { -1 };
		this.blankRhs = tokenFactory.newTokenArray(0);
		this.oovRuleScore = new RuleScore(opts.sgt, opts.tgs);

		this.oovRuleLhsList = tokenFactory.makeTokens(opts.oovRuleLhsList, true);
	}

	public List<SyncCFGRule<T>> generateOOVRules(T sourceOovTerminal,
			List<T> sourceInputBeforeOovTerminal) throws RuleException {

		ArrayList<SyncCFGRule<T>> rules = new ArrayList<SyncCFGRule<T>>(1);

		T[] copyRhs = tokenFactory.newTokenArray(1);
		copyRhs[0] = sourceOovTerminal;

		for (T oovRuleLhs : oovRuleLhsList) {
			SyncCFGRule<T> rule =
					new SyncCFGRule<T>(oovRuleLhs, copyRhs, oovRuleLhs, blankRhs, "OOV"
							+ oovCounter, alignment, oovRuleScore, null, "none", 0, tokenFactory);
			rules.add(rule);
			oovCounter++;
		}
		return rules;
	}

	public HashSet<T> getAdditionalTargetVocabulary(HashSet<T> sourceVocab) {
		return new HashSet<T>(0);
	}

}
