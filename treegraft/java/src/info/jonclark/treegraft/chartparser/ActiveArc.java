package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.DottedGrammarRule;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;

public class ActiveArc<R extends GrammarRule<T>, T extends Token> extends DottedGrammarRule<R, T> {

	public static final int DEFAULT_BACKPOINTER_LIST_SIZE = 5;
	
	private final int startIndex;
	private final int endIndex;
	private final ArrayList<Key<R, T>>[] backpointers;

	public enum OutputType {
		SOURCE_TREE, TARGET_TREE, TARGET_STRING
	}

	@SuppressWarnings("unchecked")
	public ActiveArc(int startIndex, int endIndex, int dot, R rule) {
		super(dot, rule);

		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.backpointers = (ArrayList<Key<R, T>>[]) new ArrayList[dot];
	}

	@SuppressWarnings("unchecked")
	public ActiveArc<R, T> extend(Key<R, T> key) {
		
		assert this.getEndIndex() == key.getStartIndex() : "Discontiguous arc extension.";
		
		ActiveArc<R, T> extendedArc =
				(ActiveArc<R, T>) new ActiveArc(this.getStartIndex(), key.getEndIndex(),
						this.getDot() + 1, this.getRule());
		System.arraycopy(this.backpointers, 0, extendedArc.backpointers, 0,
				this.backpointers.length);
		extendedArc.addBackpointer(this.getDot(), key);
		return extendedArc;
	}

	public void addBackpointer(int index, Key<R, T> key) {
		if (backpointers[index] == null) {
			backpointers[index] = new ArrayList<Key<R, T>>(DEFAULT_BACKPOINTER_LIST_SIZE);
		}
		backpointers[index].add(key);
	}

	public ArrayList<Key<R, T>>[] getBackpointers() {
		return backpointers;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public String toString() {
		return super.toString() + "(" + startIndex + "," + endIndex + ")";
	}
}
