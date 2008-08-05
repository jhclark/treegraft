package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.core.formatting.MonoParseFormatter;
import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.unification.Constraint;

import java.io.File;

public class MonoCFGRuleFactory<T extends Token> implements RuleFactory<MonoCFGRule<T>, T> {

	private TokenFactory<T> tokenFactory;

	public MonoCFGRuleFactory(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	public MonoCFGRule<T> makeDummyRule(T token) {
		return new MonoCFGRule<T>(token, (T[]) new Token[] { token }, new Constraint[0], "DUMMY",
				new File("null"), 0);
	}

	public ParseFormatter<MonoCFGRule<T>, T> getDefaultFormatter() {
		return new MonoParseFormatter<T>(tokenFactory);
	}

	public ParseFormatter<MonoCFGRule<T>, T>[] getDebugFormatters() {
		return new MonoParseFormatter[] {new MonoParseFormatter<T>(tokenFactory)};
	}
}
