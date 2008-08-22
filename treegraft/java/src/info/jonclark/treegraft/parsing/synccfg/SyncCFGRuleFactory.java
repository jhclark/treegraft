package info.jonclark.treegraft.parsing.synccfg;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.transduction.Transducer;
import info.jonclark.treegraft.parsing.unification.Constraint;

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
					new int[1], Grammar.DEFAULT_RULE_SCORE, new Constraint[0], "none", 0,
					tokenFactory);
		} catch (RuleException e) {
			throw new RuntimeException(e);
		}
	}

	public Transducer<SyncCFGRule<T>, T> getTransducer() {
		return new SyncCFGRuleTransducer<T>();
	}
}
