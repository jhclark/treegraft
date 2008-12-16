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
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class SyncCFGGrammarLoader<T extends Token> implements GrammarLoader<SyncCFGRule<T>, T> {

	// TODO: Fix these WMT hacks for nLine
	private static final Logger log = LogUtils.getLogger();
	private final TokenFactory<T> tokenFactory;
	private AtomicInteger nLine = new AtomicInteger(0);
	private boolean convertScoresToLog;

	public SyncCFGGrammarLoader(TokenFactory<T> tokenFactory, boolean convertScoresToLog) {
		
		this.tokenFactory = tokenFactory;
		this.convertScoresToLog = convertScoresToLog;
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

		// note: task may be null

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, encoding));
		SyncCFGRule<T> rule;
		while ((rule = nextRule(in, inputSourceName)) != null) {
			boolean lexicalInitial = rule.getRhs()[0].isTerminal();
			grammar.addRule(rule, lexicalInitial);
		}
		in.close();

		log.info("FINISHED LOADING GRAMMAR: Read " + grammar.getCandidateCount() + " and kept "
				+ grammar.getAllRules().size() + "...");
	}

	public SyncCFGRule<T> nextRule(BufferedReader in, String inputSourceName) throws IOException,
			RuleException {

		String ruleId = null;
//		StringBuilder originalString = new StringBuilder();

		// use atomic int as an int pointer
		String line;
		while ((line = in.readLine()) != null) {
			nLine.incrementAndGet();
//			originalString.append(line + "\n");

			try {

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

					RuleFeatures ruleFeatures =
							parseRuleFeatures(in, inputSourceName, nLine, targetRhs.length);

					if(convertScoresToLog) {
						ruleFeatures.sgt = ProbUtils.logProb(ruleFeatures.sgt);
						ruleFeatures.tgs = ProbUtils.logProb(ruleFeatures.tgs);
					}
					
					try {
						SyncCFGRule<T> rule =
								new SyncCFGRule<T>(sourceLhs, sourceRhs, targetLhs, targetRhs,
										ruleId, ruleFeatures.alignment, new RuleScore(
												ruleFeatures.sgt, ruleFeatures.tgs),
										ruleFeatures.getConstraintArray(), inputSourceName,
										nLine.get(), tokenFactory);

						return rule;

					} catch (RuleException e) {
						log.severe("Malformed rule (skipping rule): " + e.getMessage());
					}
				}
			} catch (Throwable t) {
				throw new RuleException("Could not read rule in " + inputSourceName + " on line "
						+ nLine.get(), t);
			}
		}

		return null;
	}

	private static class RuleFeatures {
		public double sgt;
		public double tgs;
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

	private static RuleFeatures parseRuleFeatures(BufferedReader in, String file,
			AtomicInteger nLine, int targetRhsLength)
			throws ParseException, NumberFormatException, IOException, RuleException {

		RuleFeatures ruleFeatures = new RuleFeatures(targetRhsLength);

		String ruleBeginLine = null;
		int nOpen = 0; // number of open parens at/before current line
		boolean emptyBuffer = true;

		try {
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				nLine.incrementAndGet();
//				originalString.append(line + "\n");

				try {
					if (line.length() == 0)
						continue;

					if (!line.startsWith("#") && !line.startsWith(";")) {

						// count ( and ) in each line, create a rule each time
						// we
						// hit
						// zero
						int nNewOpen = StringUtils.countOccurances(line, '(');
						int nNewClose = StringUtils.countOccurances(line, ')');

						if (line.contains("{") || line.contains("}")) {
							if (ruleBeginLine == null) {
								ruleBeginLine = file + ":" + nLine;
							} else {
								throw new ParseException("Unfinished rule starting at "
										+ ruleBeginLine, nLine.get());
							}
						}

						if (nNewOpen > 0)
							emptyBuffer = false;

						if (line.startsWith("(*sgtrule* ")) {
							String strScore =
									StringUtils.substringBetween(line, "(*sgtrule* ", ")");
							ruleFeatures.sgt = Double.parseDouble(strScore);
						} else if (line.startsWith("(*tgsrule* ")) {
							String strScore =
									StringUtils.substringBetween(line, "(*tgsrule* ", ")");
							ruleFeatures.tgs = Double.parseDouble(strScore);
						} else if (line.startsWith("(*score* ")) {
							String strScore = StringUtils.substringBetween(line, "(*score* ", ")");
							ruleFeatures.tgs = Double.parseDouble(strScore);
							ruleFeatures.sgt = 1.0;
						} else if (line.startsWith("(X") && line.contains("::Y")
								&& line.endsWith(")")) {
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
							Constraint constraint = new Constraint(line, file, nLine.get());
							ruleFeatures.addConstraint(constraint);
						}

						nOpen = nOpen + nNewOpen - nNewClose;

						if (!emptyBuffer && nOpen == 0) {
							break;
						}

					}
				} catch (Throwable t) {
					throw new RuleException("Error parsing rule in " + file + " at line "
							+ nLine.get());
				}
			}

			ruleBeginLine = file + ":" + nLine;
			if (emptyBuffer) {
				throw new ParseException("No rule features found at " + ruleBeginLine
						+ ". At mininimum, each SCFG rule must be followed by ()", nLine.get());
			}

			if (nOpen > 0) {
				if (ruleBeginLine == null) {
					ruleBeginLine = file + ":" + nLine;
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
		List<T> rhsToks = new ArrayList<T>();

		for (String rhsElement : rhs) {
			if (rhsElement.startsWith("\"")) {

				// quoted string => lexical item (terminal)

				// we have to tokenize this since some people include multiple
				// tokens inside one quoted phrase...
				String phraseNoQuotes =
						rhsElement.substring(0, rhsElement.length() - 1).substring(1);
				String[] toks = StringUtils.tokenize(phraseNoQuotes);
				if (toks.length == 0) {
					// boundary case: this is a single space
					toks = new String[] { " " };
				}
				for (String terminal : toks) {
					T tok = tokenFactory.makeToken(terminal, true);
					rhsToks.add(tok);
				}
			} else {
				T tok = tokenFactory.makeToken(rhsElement, false);
				rhsToks.add(tok);
			}
		}

		return rhsToks.toArray(tokenFactory.newTokenArray(rhsToks.size()));
	}
}
