package info.jonclark.treegraft.core.transduction;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.List;

public class Vertex<R extends GrammarRule<T>, T extends Token> {

	private R rule;
	private int startIndex;
	private int endIndex;
	private List<Vertex<R, T>>[] children;
	private T word;

	public Vertex(int startIndex, int endIndex, R rule, T terminal) {
		this.endIndex = endIndex;
		this.rule = rule;
		this.startIndex = startIndex;
		this.word = terminal;
		this.children = new List[0];
	}

	public Vertex(int startIndex, int endIndex, R rule, T nonTerminal, List<Vertex<R, T>>[] children) {
		this.children = children;
		this.endIndex = endIndex;
		this.rule = rule;
		this.startIndex = startIndex;
		this.word = nonTerminal;
	}
}
