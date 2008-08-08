package info.jonclark.treegraft.core.synccfg;

import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter.OutputType;
import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.rules.RuleException;
import info.jonclark.treegraft.core.rules.RuleFactory;
import info.jonclark.treegraft.core.scoring.BasicScorer;
import info.jonclark.treegraft.core.scoring.ParseScorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.unification.Constraint;

import java.io.File;

/**
 * A means for handling on-the-fly creation of dummy rules for terminal symbols
 * during the parsing process and for defining how partial parse output should
 * be formatted for use in debugging for <code>SyncCFGRules</code>.
 * 
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class SyncCFGRuleFactory<T extends Token> implements RuleFactory<SyncCFGRule<T>, T> {

	private TokenFactory<T> tokenFactory;
	private ParseScorer<SyncCFGRule<T>, T> scorer = new BasicScorer<SyncCFGRule<T>, T>();

	/**
	 * Creates a new <code>SyncCFGRuleFactory</code>.
	 * 
	 * @param tokenFactory
	 *            a <code>TokenFactory</code> of the current token type
	 */
	public SyncCFGRuleFactory(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public SyncCFGRule<T> makeDummyRule(T token) {
		try {
			return new SyncCFGRule<T>(token, (T[]) new Token[] { token }, null, null, null,
					new int[1], Grammar.DEFAULT_RULE_SCORE, new Constraint[0], new File("null"), 0);
		} catch (RuleException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ParseFormatter<SyncCFGRule<T>, T> getDefaultFormatter() {
		return new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, scorer, true);
	}

	/**
	 * {@inheritDoc}
	 */
	public ParseFormatter<SyncCFGRule<T>, T>[] getDebugFormatters() {
		return new ParseFormatter[] {
				new SyncParseFormatter<T>(tokenFactory, OutputType.SOURCE_TREE, scorer, false),
				new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, scorer, false) };
	}

}
