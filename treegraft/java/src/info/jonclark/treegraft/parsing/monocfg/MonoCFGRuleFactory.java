package info.jonclark.treegraft.parsing.monocfg;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.transduction.Transducer;
import info.jonclark.treegraft.parsing.unification.Constraint;

import java.io.File;

/**
 * A means for handling on-the-fly creation of dummy rules for terminal symbols
 * during the parsing process and for defining how partial parse output should
 * be formatted for use in debugging for <code>MonoCFGRules</code>.
 * 
 * @author Jonathan Clark
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class MonoCFGRuleFactory<T extends Token> implements RuleFactory<MonoCFGRule<T>, T> {

	private TokenFactory<T> tokenFactory;

	/**
	 * Creates a new <code>MonoCFGRuleFactory</code>.
	 * 
	 * @param tokenFactory
	 *            a <code>TokenFactory</code> of the current token type
	 */
	public MonoCFGRuleFactory(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	public MonoCFGRule<T> makeDummyRule(T token) {
		return new MonoCFGRule<T>(token, (T[]) new Token[] { token }, new Constraint[0], "DUMMY",
				tokenFactory);
	}

	public Transducer<MonoCFGRule<T>, T> getTransducer() {
		return new MonoCFGRuleTransducer<T>();
	}
}
