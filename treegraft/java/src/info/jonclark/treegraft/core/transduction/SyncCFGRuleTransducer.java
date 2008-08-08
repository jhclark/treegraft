package info.jonclark.treegraft.core.transduction;

import info.jonclark.treegraft.chartparser.ActiveArc;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.synccfg.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.List;

public class SyncCFGRuleTransducer<T extends Token> extends Transducer<SyncCFGRule<T>, T> {

	public enum OutputType {
		SOURCE, TARGET;
	}
	private OutputType type;

	public SyncCFGRuleTransducer(OutputType type) {
		this.type = type;
	}

	public Vertex<SyncCFGRule<T>, T> transduce(Key<SyncCFGRule<T>, T> sourceKey) {

		ActiveArc<SyncCFGRule<T>, T> sourceArc = sourceKey.getActiveArc();

		// transduce the terminal or non-terminal symbol
		T transducedWord;
		if (type == OutputType.SOURCE) {
			transducedWord = sourceKey.getRule().getLhs();
		} else if (type == OutputType.TARGET) {
			transducedWord = sourceKey.getRule().getTargetLhs();
		} else {
			throw new RuntimeException("Unknown type: " + type);
		}

		Vertex<SyncCFGRule<T>, T> target;
		if (sourceKey.isTerminal()) {
			target =
					new Vertex<SyncCFGRule<T>, T>(sourceArc.getStartIndex(),
							sourceArc.getEndIndex(), sourceKey.getRule(), transducedWord);
		} else {

			List<Vertex<SyncCFGRule<T>, T>>[] children;
			if (type == OutputType.SOURCE) {
				children = new List[sourceKey.getRule().getRhs().length];
			} else {
				children = new List[sourceKey.getRule().getTargetRhs().length];
			}
			for (int i = 0; i < children.length; i++) {
				children[i] = new ArrayList<Vertex<SyncCFGRule<T>, T>>();
			}

			// TODO: For source side, just copy over backpointers by turning
			// them into vertices
			
			// TODO: For target side, we have to re-order things, create new
			// vertices for inserted items, and then turn backpointers into
			// vertices

			target =
					new Vertex<SyncCFGRule<T>, T>(sourceArc.getStartIndex(),
							sourceArc.getEndIndex(), sourceKey.getRule(), transducedWord, children);
		}

		return target;
	}
}
