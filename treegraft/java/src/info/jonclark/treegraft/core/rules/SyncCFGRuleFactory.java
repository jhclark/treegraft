package info.jonclark.treegraft.core.rules;

import info.jonclark.treegraft.chartparser.ActiveArc.OutputType;
import info.jonclark.treegraft.core.Grammar;
import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.formatting.SyncParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.unification.Constraint;

import java.io.File;

public class SyncCFGRuleFactory<T extends Token> implements RuleFactory<SyncCFGRule<T>, T> {

	private TokenFactory<T> tokenFactory;

	public SyncCFGRuleFactory(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	public SyncCFGRule<T> makeDummyRule(T token) {
		return new SyncCFGRule<T>(token, (T[]) new Token[] { token }, null, null, null, new int[1],
				Grammar.DEFAULT_RULE_SCORE, new Constraint[0], new File("null"), 0);
	}

	public ParseFormatter<SyncCFGRule<T>, T> getDefaultFormatter() {

		// TODO: Add top level function that can invoke multiple formatters, one
		// after the other?
		// e.g. to get both source and target using a single formatter?
		return new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, true);
	}

	public ParseFormatter<SyncCFGRule<T>, T>[] getDebugFormatters() {
		return new ParseFormatter[] {
				new SyncParseFormatter<T>(tokenFactory, OutputType.SOURCE_TREE, false),
				new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, false) };
	}

}
