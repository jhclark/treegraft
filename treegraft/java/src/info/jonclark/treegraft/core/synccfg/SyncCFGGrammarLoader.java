package info.jonclark.treegraft.core.synccfg;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.rules.RuleException;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.unification.Constraint;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SyncCFGGrammarLoader {

	private static final Logger log = LogUtils.getLogger();

	/**
	 * Reads in a synchronous grammar file (see included data files for example
	 * format).
	 * 
	 * @throws RuleException
	 */
	public static <T extends Token> Grammar<SyncCFGRule<T>, T> loadSyncGrammar(File file,
			TokenFactory<T> tokenFactory, HashSet<T> vocabulary) throws IOException, ParseException {

		Grammar<SyncCFGRule<T>, T> grammar =
				new Grammar<SyncCFGRule<T>, T>(tokenFactory, Grammar.DEFAULT_START_SYMBOLS,
						vocabulary);

		BufferedReader in = new BufferedReader(new FileReader(file));

		String ruleId = null;

		// use atomic int as an int pointer
		AtomicInteger nLine = new AtomicInteger(0);
		String line;
		while ((line = in.readLine()) != null) {
			nLine.incrementAndGet();

			// replace other forms of whitespace
			if (line.contains("\t"))
				line = line.replace('\t', ' ');

			if (line.trim().equals("")) {
				// skip blank lines
				continue;
			} else if (line.trim().startsWith(";")) {
				// skip comment lines
				continue;

			} else if (line.startsWith("{")) {
				ruleId = StringUtils.substringBetween(line, "{", "}");

			} else if (line.contains("->")) {

				// TODO: More error checking
				// TODO: Check for duplicate rules

				// parse synchronous rule
				String strSourceLhs = StringUtils.substringBefore(line, "::").trim();
				String remaining = StringUtils.substringAfter(line, "::");

				boolean markedAsLexical;
				String strTargetLhs;

				String lhs = StringUtils.substringBefore(line, "[");

				if (lhs.contains(" |: ")) {
					strTargetLhs = StringUtils.substringBefore(remaining, " |: ").trim();
					remaining = StringUtils.substringAfter(line, " |: ");
					markedAsLexical = true;
				} else if (lhs.contains(" : ")) {
					strTargetLhs = StringUtils.substringBefore(remaining, " : ").trim();
					remaining = StringUtils.substringAfter(line, " : ");
					markedAsLexical = false;
				} else {

					strTargetLhs = StringUtils.substringBefore(remaining, "[").trim();
					remaining = StringUtils.substringAfter(line, "[", true);
					markedAsLexical = false;
				}

				String strSourceRhs = StringUtils.substringBefore(remaining, "->").trim();
				String strTargetRhs = StringUtils.substringAfter(remaining, "->").trim();

				// remove outer brackets and turn into Tokens
				strSourceRhs = strSourceRhs.substring(1, strSourceRhs.length() - 1);
				strTargetRhs = strTargetRhs.substring(1, strTargetRhs.length() - 1);

				T sourceLhs = tokenFactory.makeToken(strSourceLhs, false);
				T targetLhs = tokenFactory.makeToken(strTargetLhs, false);
				T[] sourceRhs = tokenizeRhs(strSourceRhs, tokenFactory);
				T[] targetRhs = tokenizeRhs(strTargetRhs, tokenFactory);

				boolean lexicalInitial = sourceRhs[0].isTerminal();

				RuleFeatures ruleFeatures = parseRuleFeatures(in, file, nLine, targetRhs.length);

				try {
					SyncCFGRule<T> rule =
							new SyncCFGRule<T>(sourceLhs, sourceRhs, targetLhs, targetRhs, ruleId,
									ruleFeatures.alignment, ruleFeatures.score,
									ruleFeatures.getConstraintArray(), file, nLine.get(),
									tokenFactory);

					grammar.addRule(rule, lexicalInitial);
				} catch (RuleException e) {
					log.severe("Rule exception (skipping rule): " + e.getMessage());
				}

			}
		}

		in.close();

		log.info("FINISHED LOADING GRAMMAR: Read " + grammar.getCandidateCount() + " and kept "
				+ grammar.getAllRules().size() + "...");

		return grammar;
	}

	private static class RuleFeatures {
		public double score = Grammar.DEFAULT_RULE_SCORE;
		public int[] alignment;
		private ArrayList<Constraint> constraints;

		public RuleFeatures(int alignmentLength) {
			alignment = new int[alignmentLength];
			for (int i = 0; i < alignment.length; i++) {
				alignment[i] = -1;
			}
		}

		public void addConstraint(Constraint constraint) {
			if (constraints == null)
				constraints = new ArrayList<Constraint>();
			constraints.add(constraint);
		}

		public Constraint[] getConstraintArray() {
			return constraints.toArray(new Constraint[constraints.size()]);
		}
	}

	private static RuleFeatures parseRuleFeatures(BufferedReader in, File ruleFile,
			AtomicInteger nLine, int targetRhsLength) throws ParseException, NumberFormatException,
			IOException {

		RuleFeatures ruleFeatures = new RuleFeatures(targetRhsLength);

		String ruleBeginLine = null;
		int nOpen = 0; // number of open parens at/before current line
		boolean emptyBuffer = true;

		try {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				nLine.incrementAndGet();

				if (line.length() == 0)
					continue;

				if (!line.startsWith("#") && !line.startsWith(";")) {

					// count ( and ) in each line, create a rule each time we
					// hit
					// zero
					int nNewOpen = StringUtils.countOccurances(line, '(');
					int nNewClose = StringUtils.countOccurances(line, ')');

					if (line.contains("{") || line.contains("}")) {
						if (ruleBeginLine == null) {
							ruleBeginLine = ruleFile.getAbsolutePath() + ":" + nLine;
						} else {
							throw new ParseException(
									"Unfinished rule starting at " + ruleBeginLine, nLine.get());
						}
					}

					if (nNewOpen > 0)
						emptyBuffer = false;

					if (line.startsWith("(*score* ")) {
						String strScore = StringUtils.substringBetween(line, "(*score* ", ")");
						ruleFeatures.score = Double.parseDouble(strScore);
					} else if (line.startsWith("(X") && line.contains("::Y") && line.endsWith(")")) {
						String strX = StringUtils.substringBetween(line, "(X", "::Y");
						String strY = StringUtils.substringBetween(line, "::Y", ")");
						int x = Integer.parseInt(strX);
						int y = Integer.parseInt(strY);

						// convert 1-based to 0-based alignments
						if (y - 1 >= ruleFeatures.alignment.length)
							System.out.println("ouch");
						ruleFeatures.alignment[y - 1] = x - 1;
					} else {
						// TODO: Implement constraints
						Constraint constraint = new Constraint(line, ruleFile, nLine.get());
						ruleFeatures.addConstraint(constraint);
					}

					nOpen = nOpen + nNewOpen - nNewClose;

					if (!emptyBuffer && nOpen == 0) {
						break;
					}
				}
			}

			ruleBeginLine = ruleFile.getAbsolutePath() + ":" + nLine;
			if (emptyBuffer) {
				throw new ParseException("No rule features found at " + ruleBeginLine
						+ ". At mininimum, each SCFG rule must be followed by ()", nLine.get());
			}

			if (nOpen > 0) {
				if (ruleBeginLine == null) {
					ruleBeginLine = ruleFile.getAbsolutePath() + ":" + nLine;
				} else {
					throw new ParseException("Unfinished rule starting at " + ruleBeginLine,
							nLine.get());
				}
			}
		} catch (ParseException e) {
			throw new ParseException("ParseException while reading rule at " + ruleBeginLine + "\n"
					+ StringUtils.getStackTrace(e), -1);
		}

		return ruleFeatures;
	}

	public static <T extends Token> T[] tokenizeRhs(String strRhs, TokenFactory<T> tokenFactory)
			throws ParseException {

		ArrayList<String> rhs =
				StringUtils.tokenizeEscapedQuotedValues(strRhs, " ", "\"", "\"", "\\", false, true,
						Integer.MAX_VALUE);
		String[] rhsArr = rhs.toArray(new String[rhs.size()]);
		T[] rhsToks = tokenFactory.newTokenArray(rhsArr.length);
		for (int i = 0; i < rhsToks.length; i++) {

			if (rhsArr[i].startsWith("\"")) {
				// quoted string => lexical item (terminal)
				String str = rhsArr[i].substring(0, rhsArr[i].length() - 1).substring(1);
				rhsToks[i] = tokenFactory.makeToken(str, true);
			} else {
				// unquoted string => non-terminal
				rhsToks[i] = tokenFactory.makeToken(rhsArr[i], false);
			}
		}
		return rhsToks;
	}
}
