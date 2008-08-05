package info.jonclark.treegraft.core;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

/**
 * A valid parse through the sentence
 */
public class Parse<R extends GrammarRule<T>, T extends Token> {

	private Key<R, T> root;
	private String lispTree;

	public Parse(String lispTree) {
		this.lispTree = lispTree;
	}

	public void setRoot(Key<R, T> root) {
		this.root = root;
	}

	public Key<R, T> getRoot() {
		return root;
	}

//	public String getLispConstituentStructure() {
//		return "()";
//	}
//
//	public String getLispFeatureStructure() {
//		return "()";
//	}
//
//	public String getLispPhiMapping() {
//		return "";
//	}
//
//	public String getLatexConstituentStructure() {
//		return "";
//	}
//
//	public String getLatexFeatureStructure() {
//		return "";
//	}

	public String toString() {
		return lispTree;
	}
}
