package info.jonclark.treegraft.core.monocfg;

import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

public class MonoCFGGrammarLoader {

	/**
	 * Reads in a monolingual grammar file (see included data files for an
	 * example format).
	 */
	public static <T extends Token> Grammar<MonoCFGRule<T>, T> loadMonoGrammar(File file,
			TokenFactory<T> tokenFactory) throws IOException, ParseException {

		Grammar<MonoCFGRule<T>, T> grammar =
				new Grammar<MonoCFGRule<T>, T>(tokenFactory, Grammar.DEFAULT_START_SYMBOLS, null);

		BufferedReader in = new BufferedReader(new FileReader(file));

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
				T[] rhs = SyncCFGGrammarLoader.tokenizeRhs(after, tokenFactory);

				MonoCFGRule<T> rule =
						new MonoCFGRule<T>(lhs, rhs, null, id, file, nLine, tokenFactory);
				grammar.addRule(rule, false);

				// parse lexical rules
			} else if (line.contains("->")) {
				String id = line;
				String pos = StringUtils.substringBefore(line, "->").trim();
				T lhs = tokenFactory.makeToken(pos, false);
				String word = StringUtils.substringAfter(line, "->").trim();
				T rhsWord = tokenFactory.makeToken(word, true);
				T[] rhsArr = (T[]) new Token[] { rhsWord };

				MonoCFGRule<T> newRule =
						new MonoCFGRule<T>(lhs, rhsArr, new Constraint[0], id, file, nLine,
								tokenFactory);
				grammar.addRule(newRule, true);
			}
		}

		in.close();

		return grammar;
	}
}
