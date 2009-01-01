package info.jonclark.treegraft.parsing.monocfg;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.HashMap;

public class MonoCFGRuleTransducer<T extends Token> implements Transducer<MonoCFGRule<T>, T> {

	// TODO: Optimize this monotonic alignment map?
	private final static HashMap<Integer, int[]> monotonicAlignments =
			new HashMap<Integer, int[]>();

	public MonoCFGRuleTransducer() {
	}

	public int[] getTargetToSourceRhsAlignment(MonoCFGRule<T> rule) {
		return getMonotonicAlignment(rule.getRhs().length);
	}

	public T transduceLhs(MonoCFGRule<T> rule) {
		return rule.getLhs();
	}

	public T[] transduceRhs(MonoCFGRule<T> rule) {
		return rule.getRhs();
	}

	public static int[] getMonotonicAlignment(int length) {
		int[] alignment = monotonicAlignments.get(length);
		if (alignment == null) {
			alignment = new int[length];
			for (int i = 0; i < alignment.length; i++)
				alignment[i] = i;
		}
		return alignment;
	}
}
