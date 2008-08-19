package info.jonclark.treegraft.core.forestunpacking.parses;

import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;

public class Tree<T extends Token> {

	private T label;
	private Scores scores;
	private Tree<T>[] children;

	public Tree(T label, Scores scores, int nChildren) {
		this.label = label;
		this.scores = scores;
		this.children = new Tree[nChildren];
	}

	public Tree(Tree<T> other) {
		this.label = other.label;
		this.scores = other.scores;
		this.children = new Tree[other.children.length];
		System.arraycopy(other.children, 0, this.children, 0, other.children.length);
	}

	public boolean isTerminal() {
		return (children.length == 0);
	}

	public T getLabel() {
		return label;
	}

	public void addChild(int index, Tree<T> child) {
		children[index] = child;
	}

	public void setScores(Scores scores) {
		this.scores = scores;
	}

	public Tree<T>[] getChildren() {
		return children;
	}

	// null for terminals
	public Scores getScores() {
		return scores;
	}

	public String toString(TreeFormatter<T> formatter) {
		StringBuilder builder = new StringBuilder();
		toString(formatter, builder);
		return builder.toString().trim();
	}

	public void toString(TreeFormatter<T> formatter, StringBuilder builder) {

		if (isTerminal()) {
			builder.append(formatter.formatTerminal(label));
		} else {
			builder.append(formatter.formatNonterminalBefore(label, scores));
			for (Tree<T> child : children) {
				child.toString(formatter, builder);
			}
			builder.append(formatter.formatNonterminalAfter(label, scores));
		}
	}
}
