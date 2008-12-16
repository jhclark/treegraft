package info.jonclark.treegraft.parsing.monocfg;

import info.jonclark.stat.TaskListener;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.unification.Constraint;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;

public class MonoCFGGrammarLoader<T extends Token> implements GrammarLoader<MonoCFGRule<T>, T> {

	private final TokenFactory<T> tokenFactory;
	private final boolean convertScoresToLog;

	public MonoCFGGrammarLoader(TokenFactory<T> tokenFactory, boolean convertScoresToLog) {
		this.tokenFactory = tokenFactory;
		this.convertScoresToLog = convertScoresToLog;
	}

	/**
	 * Reads in a monolingual grammar file (see included data files for an
	 * example format).
	 */
	public void loadGrammar(Grammar<MonoCFGRule<T>, T> grammar, InputStream stream,
			String inputSourceName, String encoding, TaskListener task) throws IOException,
			RuleException {

		BufferedReader in = new BufferedReader(new InputStreamReader(stream));

		int nLine = 0;
		String line;
		while ((line = in.readLine()) != null) {
			nLine++;
			line = line.trim();

			String arrow = "";
			if (line.equals("")) {
				// skip blank lines
				continue;

			} else if (line.startsWith(";")) {
				// skip comment lines
				continue;

			} else if (line.contains("=>")) {

				// parse monolingual non-terminal rules
				String id = line;
				String strLhs = StringUtils.substringBefore(line, "=>").trim();
				T lhs = tokenFactory.makeToken(strLhs, false);
				String after = StringUtils.substringAfter(line, "=>").trim();
				T[] rhs;
				try {
					rhs = SyncCFGGrammarLoader.tokenizeRhs(after, tokenFactory);
				} catch (ParseException e) {
					throw new RuleException("Invalid rule at " + inputSourceName + " on line "
							+ nLine);
				}

				MonoCFGRule<T> rule = new MonoCFGRule<T>(lhs, rhs, null, id, tokenFactory);
				grammar.addRule(rule, false);

				// parse lexical rules
			} else if (line.contains("->")) {
				String id = line;
				String pos = StringUtils.substringBefore(line, "->").trim();
				T lhs = tokenFactory.makeToken(pos, false);
				String word = StringUtils.substringAfter(line, "->").trim();
				T rhsWord = tokenFactory.makeToken(word, true);
				T[] rhsArr = (T[]) new Token[] { rhsWord };

				if(convertScoresToLog) {
					// TODO: Scores
				}
				
				MonoCFGRule<T> newRule =
						new MonoCFGRule<T>(lhs, rhsArr, new Constraint[0], id, tokenFactory);
				grammar.addRule(newRule, true);
			}
		}

		in.close();
	}
}
