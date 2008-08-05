package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

public class DottedGrammarRule<R extends GrammarRule<T>, T extends Token> {

	protected final int dot;
	protected final R rule;

	public DottedGrammarRule(int dot, R rule) {

		assert rule != null : "null grammar rule.";
		this.dot = dot;
		this.rule = rule;
	}

	public int getDot() {
		return dot;
	}

	public R getRule() {
		return rule;
	}

	/**
	 * Returns NULL if no more constituents are needed
	 * 
	 * @return
	 */
	public T getNeededSymbol() {
		if (dot >= rule.getRhs().length) {
			return null;
		} else {
			T needed = rule.getRhs()[dot];
			return needed;
		}
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(rule.getLhs().getId());

		T[] rhs = rule.getRhs();
		if (rhs.length > 0) {
			builder.append(" -> ");
		}

		for (int i = 0; i < rhs.length; i++) {
			if (i == dot) {
				builder.append("* ");
			}
			builder.append(rhs[i].getId() + " ");
		}
		if (rhs.length == dot) {
			builder.append("* ");
		}
		return "[" + builder.toString() + "]";
	}

	public String toString(TokenFactory<T> symbolFactory) {
		StringBuilder builder = new StringBuilder();
		builder.append(symbolFactory.getTokenAsString(rule.getLhs()));

		T[] rhs = rule.getRhs();
		if (rhs.length > 0) {
			builder.append(" -> ");
		}

		for (int i = 0; i < rhs.length; i++) {
			if (i == dot) {
				builder.append("* ");
			}
			builder.append(symbolFactory.getTokenAsString(rhs[i]) + " ");
		}
		if (rhs.length == dot) {
			builder.append("* ");
		}
		return "[" + builder.toString() + "]";
	}
}
