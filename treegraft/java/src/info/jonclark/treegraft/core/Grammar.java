package info.jonclark.treegraft.core;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.MonoCFGRule;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Grammar<R extends GrammarRule<T>, T extends Token> {

	public static final double DEFAULT_RULE_SCORE = 1.0;

	private List<R> emptyRuleList;
	private HashMap<T, ArrayList<R>> rules = new HashMap<T, ArrayList<R>>();
	private HashMap<T, ArrayList<R>> terminalInitialRules = new HashMap<T, ArrayList<R>>();
	private HashSet<T> startSymbols = new HashSet<T>();
	private static int nRules = 0;

	private final TokenFactory<T> tokenFactory;

	public Grammar(TokenFactory<T> tokenFactory) {
		this.tokenFactory = tokenFactory;

		startSymbols.add(tokenFactory.makeToken("S", false));
	}

	public static <T extends Token> void loadMonoGrammar(File file, TokenFactory<T> tokenFactory,
			Grammar<MonoCFGRule<T>, T> grammar) throws IOException, ParseException {

		BufferedReader in = new BufferedReader(new FileReader(file));
		ArrayList<MonoCFGRule<T>> ruleList = new ArrayList<MonoCFGRule<T>>();
		grammar.emptyRuleList = new ArrayList<MonoCFGRule<T>>(0);

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
				T[] rhs = tokenizeRhs(after, tokenFactory);

				MonoCFGRule<T> rule = new MonoCFGRule<T>(lhs, rhs, null, id, file, nLine);
				ruleList.add(rule);

				ArrayList<MonoCFGRule<T>> existingRules = grammar.rules.get(rhs[0]);
				if (existingRules == null) {
					existingRules = new ArrayList<MonoCFGRule<T>>();
					grammar.rules.put(rhs[0], existingRules);
				}
				existingRules.add(rule);

				// parse lexical rules
			} else if (line.contains("->")) {
				String id = line;
				String pos = StringUtils.substringBefore(line, "->").trim();
				T lhs = tokenFactory.makeToken(pos, false);
				String word = StringUtils.substringAfter(line, "->").trim();
				T rhsWord = tokenFactory.makeToken(word, true);
				T[] rhsArr = (T[]) new Token[] { rhsWord };

				MonoCFGRule<T> newRule =
						new MonoCFGRule<T>(lhs, rhsArr, new Constraint[0], id, file, nLine);

				ArrayList<MonoCFGRule<T>> existingRules =
						grammar.terminalInitialRules.get(rhsArr[0]);
				if (existingRules == null) {
					existingRules = new ArrayList<MonoCFGRule<T>>();
					grammar.terminalInitialRules.put(rhsArr[0], existingRules);
				}
				existingRules.add(newRule);
			}
		}

		in.close();
	}

	public static <T extends Token> void loadSyncGrammar(File file, TokenFactory<T> tokenFactory,
			Grammar<SyncCFGRule<T>, T> grammar) throws IOException, ParseException {

		BufferedReader in = new BufferedReader(new FileReader(file));
		ArrayList<SyncCFGRule<T>> ruleList = new ArrayList<SyncCFGRule<T>>();
		grammar.emptyRuleList = new ArrayList<SyncCFGRule<T>>(0);

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
				nRules++;
				if (nRules % 100000 == 0)
					System.out.println("Read " + nRules + " so far...");

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

				SyncCFGRule<T> rule =
						new SyncCFGRule<T>(sourceLhs, sourceRhs, targetLhs, targetRhs,
								ruleId, ruleFeatures.alignment, ruleFeatures.score,
								ruleFeatures.getConstraintArray(), file, nLine.get());
				ruleList.add(rule);

				if (lexicalInitial) {
					T lexicalToken = sourceRhs[0];

					ArrayList<SyncCFGRule<T>> existingRules =
							grammar.terminalInitialRules.get(lexicalToken);
					if (existingRules == null) {
						existingRules = new ArrayList<SyncCFGRule<T>>();
						grammar.terminalInitialRules.put(lexicalToken, existingRules);
					}
					existingRules.add(rule);

				} else {
					ArrayList<SyncCFGRule<T>> existingRules = grammar.rules.get(sourceRhs[0]);
					if (existingRules == null) {
						existingRules = new ArrayList<SyncCFGRule<T>>();
						grammar.rules.put(sourceRhs[0], existingRules);
					}
					existingRules.add(rule);
				}

			}
		}

		in.close();
	}

	private static class RuleFeatures {
		public double score = DEFAULT_RULE_SCORE;
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

	/**
	 * Gets rules whose source LHS begin with the specified terminal token.
	 * 
	 * @param word
	 * @return
	 */
	public List<R> getTerminalInitialRules(T word) {
		List<R> rules = this.terminalInitialRules.get(word);
		if (rules == null) {
			return emptyRuleList;
		} else {
			return rules;
		}
	}

	/**
	 * Gets rules whose source LHS begins with the specified key (which contains
	 * a non-terminal token).
	 * 
	 * @param key
	 * @return
	 */
	public List<R> getRulesStartingWith(Key<R, T> key) {
		// if (useTopDownPredictions) {}
		List<R> result = rules.get(key.getLhs());
		if (result == null) {
			return emptyRuleList;
		} else {
			return result;
		}
	}

	public boolean isStartSymbol(T token) {
		return startSymbols.contains(token);
	}
}
