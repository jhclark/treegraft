package info.jonclark.treegraft.parsing.grammar;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;
import info.jonclark.util.HashUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Contains (and can read in) the rules with which input sequences will be
 * parsed.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class Grammar<R extends GrammarRule<T>, T extends Token> {

	public static final String[] DEFAULT_START_SYMBOLS = { "S" };

	private static final Logger log = LogUtils.getLogger();

	private final List<R> emptyRuleList = Collections.EMPTY_LIST;
	private HashMap<T, ArrayList<R>> nonterminalInitialRules = new HashMap<T, ArrayList<R>>();
	private HashMap<T, ArrayList<R>> terminalInitialRules = new HashMap<T, ArrayList<R>>();
	private ArrayList<R> allRules = new ArrayList<R>();
	private HashSet<T> startSymbols = new HashSet<T>();
	private HashSet<T> vocabulary;
	private int nCandidates = 0;

	private HashSet<T> filterLHSTokens;
	private HashSet<T> filterRHSTokens;

	/**
	 * @param tokenFactory
	 * @param startSymbols
	 * @param vocabulary
	 *            The vocabulary of source-side terminal symbols to which the
	 *            rules should be filtered. NULL implies that all rules should
	 *            be accepted
	 * @param filterLHSTokens
	 *            The LHS's which will trigger the exclusion of a rule.
	 * @param filterRHSTokens
	 *            The RHS's, any one of which will trigger the exclusion of a
	 *            rule.
	 */
	public Grammar(TokenFactory<T> tokenFactory, String[] startSymbols, HashSet<T> vocabulary,
			HashSet<T> filterLHSTokens, HashSet<T> filterRHSTokens) {

		for (String startSymbol : startSymbols) {
			this.startSymbols.add(tokenFactory.makeToken(startSymbol, false));
		}
		this.vocabulary = vocabulary;
		this.filterLHSTokens = filterLHSTokens;
		this.filterRHSTokens = filterRHSTokens;

		if (filterLHSTokens == null)
			this.filterLHSTokens = new HashSet<T>();
		if (filterRHSTokens == null)
			this.filterRHSTokens = new HashSet<T>();
	}

	private Grammar() {
		this.vocabulary = null;
	}

	/**
	 * Filters this <code>Grammar</code> to a specified vocabulary such that
	 * every rule in the new resulting grammar contains terminals only from the
	 * specified vocabulary.
	 * 
	 * @param vocabulary
	 *            a set of terminal tokens that might be encountered during
	 *            parsing
	 * @return a new grammar filtered to the specified vocabulary
	 */
	public Grammar<R, T> filterToVocabulary(HashSet<T> vocabulary) {

		Grammar<R, T> filteredGrammar = new Grammar<R, T>();
		filteredGrammar.startSymbols = this.startSymbols;

		filterSet(vocabulary, filteredGrammar, this.nonterminalInitialRules,
				filteredGrammar.nonterminalInitialRules);
		filterSet(vocabulary, filteredGrammar, this.terminalInitialRules,
				filteredGrammar.terminalInitialRules);

		log.info("Kept " + filteredGrammar.getAllRules().size() + " / " + this.getAllRules().size()
				+ " rules.");

		return filteredGrammar;
	}

	private void filterSet(HashSet<T> vocabulary, Grammar<R, T> filteredGrammar,
			HashMap<T, ArrayList<R>> inputRuleSet, HashMap<T, ArrayList<R>> outputRuleSet) {

		for (Entry<T, ArrayList<R>> entry : inputRuleSet.entrySet()) {
			for (R rule : entry.getValue()) {

				if (ruleIsInVocabulary(vocabulary, rule)) {
					HashUtils.append(outputRuleSet, entry.getKey(), rule);
					filteredGrammar.allRules.add(rule);
				}
			}
		}
	}

	public Grammar<R, T> keepKBestRules(int k) {

		// declare 2 rules equal if their source LHS
		// and source RHS are equal
		Comparator<R> category = new Comparator<R>() {
			public int compare(R o1, R o2) {
				// int eq = o1.getLhs().compareTo(o2.getLhs());
				// if (eq != 0) {
				// return eq;
				// }
				if (o1.getRhs().length < o2.getRhs().length) {
					return -1;
				} else if (o1.getRhs().length > o2.getRhs().length) {
					return 1;
				}

				for (int i = 0; i < o1.getRhs().length; i++) {
					int eq = o1.getRhs()[i].compareTo(o2.getRhs()[i]);
					if (eq != 0) {
						return eq;
					}
				}
				return 0;
			}
		};
		Comparator<R> ranker = new Comparator<R>() {
			public int compare(R o1, R o2) {
				double s1 = o1.getRuleScores().sgt * o1.getRuleScores().tgs;
				double s2 = o2.getRuleScores().sgt * o2.getRuleScores().tgs;
				return (int) (s1 - s2);
			}
		};
		return keepKBestRules(k, category, ranker);
	}

	public Grammar<R, T> keepKBestRules(int k, Comparator<R> categoryComparator,
			Comparator<R> ranker) {

		Collections.sort(allRules, categoryComparator);

		Grammar<R, T> newGrammar = new Grammar<R, T>();
		for (int i = 0; i < allRules.size();) {
			R firstRule = allRules.get(i);
			int j = i;
			do {
				j++;
			} while (j < allRules.size()
					&& categoryComparator.compare(firstRule, allRules.get(j)) == 0);

			List<R> ruleGroup = allRules.subList(i, j);
			Collections.sort(ruleGroup, ranker);
			for (int x = 0; x < k && x < ruleGroup.size(); x++) {
				R r = ruleGroup.get(x);
				boolean lexicalInitial = r.getRhs()[0].isTerminal();
				newGrammar.addRule(r, lexicalInitial);
			}
			i = j;
		}
		return newGrammar;
	}

	private boolean ruleIsInVocabulary(HashSet<T> vocabulary, R rule) {

		boolean good = true;

		if (filterLHSTokens.contains(rule.getLhs())) {
			log.warning("FILTERING RULE: " + rule);
			good = false;
		}

		if (good) {
			for (T rhs : rule.getRhs()) {
				if (filterRHSTokens.contains(rhs)) {
					log.warning("FILTERING RULE: " + rule);
					good = false;
					break;
				}
			}
		}

		if (good) {
			for (T token : rule.getRhs()) {
				if (token.isTerminal() && (vocabulary.contains(token) == false)) {
					good = false;
					break;
				}
			}
		}

		return good;
	}

	/**
	 * Adds a rule to this grammar IF it is contained in the vocabulary passed
	 * to the constructor or NULL was passed to the constructor as the
	 * vocabulary.
	 * 
	 * @param rule
	 *            the rule that might be added to this grammar
	 * @param terminalInitial
	 *            should be true if the first symbol of the RHS of this rule is
	 *            a terminal
	 */
	public void addRule(R rule, boolean terminalInitial) {

		if (vocabulary == null || ruleIsInVocabulary(vocabulary, rule)) {
			if (terminalInitial) {
				putRule(rule, terminalInitialRules);
			} else {
				putRule(rule, nonterminalInitialRules);
			}
		}

		nCandidates++;
		if (nCandidates % 100000 == 0)
			log.fine("Read " + nCandidates + " so far and kept " + getAllRules().size() + "...");
	}

	private void putRule(R rule, HashMap<T, ArrayList<R>> ruleMap) {
		ArrayList<R> existingRules = ruleMap.get(rule.getRhs()[0]);
		if (existingRules == null) {
			existingRules = new ArrayList<R>();
			ruleMap.put(rule.getRhs()[0], existingRules);
		}
		existingRules.add(rule);
		allRules.add(rule);
		
//		System.out.println("Loading rule: " + rule.toString() + " TO " + rule.getRhs()[0]);
//		System.out.println(ruleMap.toString());
	}

	/**
	 * Gets the number of rules that were added to this <code>Grammar</code>
	 * before the internal filtering was applied.
	 * 
	 * @return
	 */
	public int getCandidateCount() {
		return nCandidates;
	}

	public HashSet<T> getTargetVocabulary(Transducer<R, T> transducer) {
		HashSet<T> targetVocabulary = new HashSet<T>();
		for (R rule : allRules) {
			T[] targetRhs = transducer.transduceRhs(rule);
			for (T token : targetRhs) {
				if (token.isTerminal()) {
					targetVocabulary.add(token);
				}
			}
		}
		return targetVocabulary;
	}

	/**
	 * Gets rules whose source LHS begin with the specified terminal token.
	 * 
	 * @param word
	 * @return
	 */
	public List<R> getTerminalInitialRules(T word) {
		List<R> rules = this.terminalInitialRules.get(word);
//		System.out.println(terminalInitialRules.toString());
//		System.out.println(word.toString());
//		System.out.println("RULES: " + rules);
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
		List<R> result = nonterminalInitialRules.get(key.getLhs());
		if (result == null) {
			return emptyRuleList;
		} else {
			return result;
		}
	}

	/**
	 * Gets a list of all rules contained in this <code>Grammar</code>
	 * 
	 * @return a list of rules
	 */
	public ArrayList<R> getAllRules() {
		return allRules;
	}

	/**
	 * Gets the top n rules with the regard to the number of keys that were
	 * directly or indirectly created due to the rule.
	 * 
	 * @param n
	 *            the number of rules to be returned
	 * @return a list of the most productive rules in the grammar
	 */
	public ArrayList<R> getNMostProductiveRules(int n) {
		Collections.sort(allRules, new Comparator<R>() {
			public int compare(R a, R b) {
				return (b.getKeysCreated() - a.getKeysCreated());
			}
		});

		ArrayList<R> list = new ArrayList<R>(n);
		for (int i = 0; i < n && i < allRules.size(); i++) {
			list.add(allRules.get(i));
		}
		return list;
	}

	public ArrayList<R> getNSlowestRules(int n) {
		Collections.sort(allRules, new Comparator<R>() {
			public int compare(R a, R b) {
				return (int) ((b.getTimeCost() - a.getTimeCost()) * 1000);
			}

		});

		ArrayList<R> list = new ArrayList<R>(n);
		for (int i = 0; i < n && i < allRules.size(); i++) {
			list.add(allRules.get(i));
		}
		return list;
	}

	/**
	 * Determines if a terminal or non-terminal token is a start symbol.
	 * 
	 * @param token
	 *            the token in question
	 * @return True if the token is a start symbol; false otherwise
	 */
	public boolean isStartSymbol(T token) {
		return startSymbols.contains(token);
	}
}
