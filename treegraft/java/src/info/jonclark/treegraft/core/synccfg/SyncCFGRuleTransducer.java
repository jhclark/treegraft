package info.jonclark.treegraft.core.synccfg;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.transduction.Transducer;

public class SyncCFGRuleTransducer<T extends Token> implements Transducer<SyncCFGRule<T>, T> {

	public SyncCFGRuleTransducer() {
	}

	public int[] getTargetToSourceRhsAlignment(SyncCFGRule<T> rule) {
		return rule.getTargetToSourceAlignment();
	}

	public T transduceLhs(SyncCFGRule<T> rule) {
		return rule.getTargetLhs();
	}

	public T[] transduceRhs(SyncCFGRule<T> rule) {
		return rule.getTargetRhs();
	}

}
