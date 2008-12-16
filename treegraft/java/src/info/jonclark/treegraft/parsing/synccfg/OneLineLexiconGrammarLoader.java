package info.jonclark.treegraft.parsing.synccfg;

import info.jonclark.log.LogUtils;
import info.jonclark.stat.TaskListener;
import info.jonclark.treegraft.core.featureimpl.RuleScore;
import info.jonclark.treegraft.core.scoring.ProbUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.unification.Constraint;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class OneLineLexiconGrammarLoader<T extends Token> implements
		GrammarLoader<SyncCFGRule<T>, T> {

	private static final Logger log = LogUtils.getLogger();
	private final TokenFactory<T> tokenFactory;

	public OneLineLexiconGrammarLoader(TokenFactory<T> tokenFactory) {

		this.tokenFactory = tokenFactory;
	}

	/**
	 * Reads in a synchronous grammar file (see included data files for example
	 * format).
	 * 
	 * @param file
	 * @param tokenFactory
	 * @param vocabulary
	 *            The vocabulary of source-side terminal symbols to which the
	 *            rules should be filtered. NULL implies that all rules should
	 *            be accepted
	 * @param filterLHSTokens
	 *            The LHS's which will trigger the exclusion of a rule.
	 * @param filterRHSTokens
	 *            The RHS's, any one of which will trigger the exclusion of a
	 *            rule.
	 * @throws RuleException
	 * @throws NumberFormatException
	 * @throws RuleException
	 */
	public void loadGrammar(Grammar<SyncCFGRule<T>, T> grammar, InputStream stream,
			String inputSourceName, String encoding, TaskListener task) throws IOException,
			RuleException {

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, encoding));

		File possibleFile = new File(inputSourceName);
		if (possibleFile.exists()) {
			int nEntries = FileUtils.countLines(possibleFile);
			task.beginTask(nEntries);
		}

		// use atomic int as an int pointer
		int nLine = 0;
		String line;
		while ((line = in.readLine()) != null) {
			nLine++;

			String[] columns = StringUtils.tokenize(line, "#");
			String ruleId = columns[0].trim();
			String strSourceLhs = columns[1].trim();
			String strTargetLhs = columns[2].trim();
			String strSourceRhs = columns[3].trim();
			String strTargetRhs = columns[4].trim();
			String strScores = columns[5].trim();

			T sourceLhs = tokenFactory.makeToken(strSourceLhs, false);
			T targetLhs = tokenFactory.makeToken(strTargetLhs, false);
			T[] sourceRhs = tokenFactory.makeTokens(StringUtils.tokenize(strSourceRhs), true);
			T[] targetRhs = tokenFactory.makeTokens(StringUtils.tokenize(strTargetRhs), true);

			String[] scores = StringUtils.tokenize(strScores);
			double tgs = Double.parseDouble(scores[0]);
			double sgt = Double.parseDouble(scores[1]);

			double logTgs = ProbUtils.logProb(tgs);
			double logSgt = ProbUtils.logProb(sgt);

			int[] alignment = new int[targetRhs.length];
			for (int i = 0; i < alignment.length; i++) {
				alignment[i] = -1;
			}

			try {
				task.recordEventCompletion();
				SyncCFGRule<T> rule =
						new SyncCFGRule<T>(sourceLhs, sourceRhs, targetLhs, targetRhs, ruleId,
								alignment, new RuleScore(logSgt, logTgs), new Constraint[0],
								inputSourceName, nLine, tokenFactory);

				grammar.addRule(rule, true);

			} catch (RuleException e) {
				log.severe("Malformed rule (skipping rule): " + e.getMessage());
			}

		}

		in.close();
		task.recordEventCompletion();

		log.info("FINISHED LOADING GRAMMAR: Read " + grammar.getCandidateCount() + " and kept "
				+ grammar.getAllRules().size() + "...");
	}
}
