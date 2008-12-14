package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.util.StringUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Q: How do I get the rule that created this (sub)-tree? <br>
 * A:
 * 
 * @author jon
 * @param <R>
 *            The "creator" of this Tree. Usually a GrammarRule type, but could
 *            be something as simple as a String or File.
 * @param <T>
 */
public class Tree<R, T extends Token> {

	private T label;
	private FeatureScores scores;
	private Tree<R, T>[] children;
	private R creator;

	public Tree(T label, R creator, FeatureScores scores, int nChildren) {
		this.label = label;
		this.creator = creator;
		this.scores = scores;
		this.children = new Tree[nChildren];
	}

	public Tree(Tree<R, T> other) {
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

	public void setLabel(T label) {
		this.label = label;
	}

	public void addChild(int index, Tree<R, T> child) {
		children[index] = child;
	}

	public void setScores(FeatureScores scores) {
		this.scores = scores;
	}

	public Tree<R, T>[] getChildren() {
		return children;
	}

	// null for terminals
	public FeatureScores getScores() {
		return scores;
	}

	/**
	 * Returns the "creator" of this (sub)-tree. Usually a rule, but could be
	 * something as simple as a String or File.
	 * 
	 * @return
	 */
	public R getCreator() {
		return creator;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (isTerminal()) {
			builder.append(label);
		} else {
			builder.append("(" + label + " ");
			for (Tree<R, T> child : children) {
				builder.append(child.toString() + " ");
			}
			builder.append(")");
		}
		return builder.toString();
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
			for (Tree<R, T> child : children) {
				child.toString(formatter, builder);
			}
			builder.append(formatter.formatNonterminalAfter(label, scores));
		}
	}

	public static <R, T extends Token> Tree<R, T> parseTree(String strTree, R creator,
			TokenFactory<T> tokenFactory) throws ParseException {

		// remove leading and trailing parens so we don't get a "null" label at
		// the root
		if (strTree.contains("(ROOT")) {
			strTree = StringUtils.substringAfter(strTree, "(ROOT ");
			strTree = StringUtils.substringBeforeLast(strTree, ")");
		}
		strTree = StringUtils.substringAfter(strTree, "(");
		strTree = StringUtils.substringBeforeLast(strTree, ")");
		return parse(strTree, creator, tokenFactory);
	}

	private static <R, T extends Token> Tree<R, T> parse(String strTree, R creator,
			TokenFactory<T> tokenFactory) throws ParseException {

		strTree = strTree.trim();

		if (strTree.contains("(") == false) {

			// pre-terminal
			String[] toks = StringUtils.tokenize(strTree);

			T preterm = tokenFactory.makeToken(toks[0], false);
			T term = tokenFactory.makeToken(toks[1], true);
			FeatureScores scores = null;

			Tree<R, T> treeNode = new Tree<R, T>(preterm, creator, scores, 1);
			Tree<R, T> childNode = new Tree<R, T>(term, creator, scores, 0);
			treeNode.addChild(0, childNode);

			return treeNode;

		} else {

			// non-terminal

			// first, get the label
			List<Tree<R, T>> children = new ArrayList<Tree<R, T>>();
			String strLabel = StringUtils.substringBefore(strTree, "(", false).trim();
			strTree = StringUtils.substringAfter(strTree, "(", true);

			int nChildStart;
			int nChildEnd = 0;
			while ((nChildStart = strTree.indexOf('(', nChildEnd)) != -1) {

				nChildEnd = StringUtils.findMatching(strTree, nChildStart, '(', ')');
				assert nChildEnd < strTree.length() : "Matching paren out of range: " + nChildEnd
						+ " for string of length " + strTree.length();

				String nestedNode = strTree.substring(nChildStart + 1, nChildEnd);

				// do not give children node labels
				Tree<R, T> child = parse(nestedNode, creator, tokenFactory);
				children.add(child);
			}

			T label = tokenFactory.makeToken(strLabel, false);
			FeatureScores scores = null;
			Tree<R, T> treeNode = new Tree<R, T>(label, creator, scores, children.size());
			for (int i = 0; i < children.size(); i++) {
				treeNode.children[i] = children.get(i);
			}
			return treeNode;
		}
	}
}
