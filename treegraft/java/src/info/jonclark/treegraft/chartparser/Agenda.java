package info.jonclark.treegraft.chartparser;

import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.LinkedList;
import java.util.Queue;

public class Agenda<R extends GrammarRule<T>, T extends Token> {

	private final Queue<Key<R, T>> q = new LinkedList<Key<R, T>>();

	public boolean isEmpty() {
		return q.isEmpty();
	}

	public void add(Key<R, T> key) {
		q.add(key);
	}

	public Key<R, T> get() {
		return q.remove();
	}
	
	public String toString() {
		return q.toString();
	}
}
