package info.jonclark.treegraft.core.grammar;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
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

	public static final double DEFAULT_RULE_SCORE = 1.0;
	public static final String[] DEFAULT_START_SYMBOLS = { "S" };

	private static final Logger log = LogUtils.getLogger();

	private final List<R> emptyRuleList = Collections.EMPTY_LIST;
	private HashMap<T, ArrayList<R>> rules = new HashMap<T, ArrayList<R>>();
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
		
		if(filterLHSTokens == null)
			this.filterLHSTokens = new HashSet<T>();
		if(filterRHSTokens == null)
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

		filterSet(vocabulary, filteredGrammar, this.rules, filteredGrammar.rules);
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
				ArrayList<R> existingRules = terminalInitialRules.get(rule.getRhs()[0]);
				if (existingRules == null) {
					existingRules = new ArrayList<R>();
					terminalInitialRules.put(rule.getRhs()[0], existingRules);
				}
				existingRules.add(rule);
				allRules.add(rule);
			} else {
				ArrayList<R> existingRules = rules.get(rule.getRhs()[0]);
				if (existingRules == null) {
					existingRules = new ArrayList<R>();
					rules.put(rule.getRhs()[0], existingRules);
				}
				existingRules.add(rule);
				allRules.add(rule);
			}
		}

		nCandidates++;
		if (nCandidates % 100000 == 0)
			log.info("Read " + nCandidates + " so far and kept " + getAllRules().size() + "...");
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
