package info.jonclark.treegraft.parsing.oov;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.featureimpl.RuleScore;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@OptionsTarget(CopyOOVHandler.CopyOrDeleteOOVHandlerOptions.class)
public class CopyOOVHandler<T extends Token> implements OutOfVocabularyHandler<SyncCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final RuleScore oovRuleScore;
	private final T[] oovRuleLhsList;
	private final int[] alignment;
	private int oovCounter = 0;

	public static class CopyOrDeleteOOVHandlerOptions implements Options {

		@Option(name = "grammar.oovHandler.oovRuleSgtLogProb", usage = "The source-given-target log probability for out of vocabulary words")
		public double sgt;

		@Option(name = "grammar.oovHandler.oovRuleTgsLogProb", usage = "The target-given-source log probability for out of vocabulary words")
		public double tgs;

		@Option(name = "grammar.oovHandler.oovRuleLhsList", usage = "An OOV rule will be created for every LHS symbol in this list each time an out of vocabulary word is encountered")
		public String[] oovRuleLhsList;
	}

	public CopyOOVHandler(CopyOrDeleteOOVHandlerOptions opts, TreegraftConfig<?, T> config) {

		this.tokenFactory = config.tokenFactory;
		this.alignment = new int[] { -1 };

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
