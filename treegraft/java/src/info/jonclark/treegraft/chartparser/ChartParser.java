package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.Grammar;
import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.RuleFactory;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A left-to-right bottom-up depth-first-search chart parser that accepts both
 * monolingual and synchronous grammars.
 * 
 * @author Jonathan Clark
 * @param <R>
 *            The rule type being used in this <code>ChartParser</code>
 * @param <T>
 *            The token type being used in this <code>ChartParser</code>
 */
public class ChartParser<R extends GrammarRule<T>, T extends Token> {

	private Grammar<R, T> grammar;
	private RuleFactory<R, T> ruleFactory;
	private static final Logger log = LogUtils.getLogger();

	public ChartParser(RuleFactory<R, T> ruleFactory, Grammar<R, T> grammar) {
		this.ruleFactory = ruleFactory;
		this.grammar = grammar;
	}

	/**
	 * Populates the parse chart with active arcs and notes any complete
	 * (grammatical) parses. This method takes in a chart as an argument with
	 * the idea that charts can be passed through several parsing mechanisms and
	 * further populated by other methods though this functionality is currently
	 * untested.
	 * 
	 * @param input
	 * @param chart
	 */
	public void parse(T[] input, Chart<R, T> chart) {

		Agenda<R, T> agenda = new Agenda<R, T>();
		ActiveArcManager<R, T> arcMan = new ActiveArcManager<R, T>(input.length);
		// ConstraintEngine constraintEngine = new ConstraintEngine();

		// step 1
		int i = 0;

		do {

			// step 2 -- turn tokens into keys and/or active arcs
			while (agenda.isEmpty() && i < input.length) {

				List<R> matchingLexicalRules = grammar.getTerminalInitialRules(input[i]);
				for (final R lexicalRule : matchingLexicalRules) {

					// make a new arc due to a lexical input
					ActiveArc<R, T> ruleArc = new ActiveArc<R, T>(i, i + 1, 1, lexicalRule);
					arcMan.add(ruleArc);

					Key<R, T> dummyKey = makeDummyKey(input, i);
					ruleArc.addBackpointer(0, dummyKey);
				}

				// extend terminals in arcs created by previous tokens
				// NOTE: Arcs we just added are extended by default
				Key<R, T> dummyKey = makeDummyKey(input, i);
				arcMan.extendArcs(dummyKey);

				ActiveArc<R, T>[] completedArcs = arcMan.getAndClearCompletedArcs();
				for (final ActiveArc<R, T> completedArc : completedArcs) {
					agenda.add(new Key<R, T>(completedArc, null));
				}

				i++;
			}

			if (agenda.isEmpty()) {
				// we've extended everything we can
				break;
			}

			// step 3
			Key<R, T> key = agenda.get();
			key.startTimer();

			log.info("PROCESSING KEY: " + key + " from rule " + key.getRule().toString());

			// TODO: use timers for partially completed arcs too

			// step 4 -- add arcs beginning with this key
			List<R> rules = grammar.getRulesStartingWith(key);
			for (final R rule : rules) {

				// create arc with dot after the first RHS constituent
				ActiveArc<R, T> extendedArc =
						new ActiveArc<R, T>(key.getStartIndex(), key.getEndIndex(), 1, rule);

				// fill in the key that shows why the first RHS constituent is
				// complete
				extendedArc.addBackpointer(0, key);
				arcMan.add(extendedArc);
			}

			// step 5 -- extend all arcs using the current key
			arcMan.extendArcs(key);

			// step 6 -- check for completed arcs
			ActiveArc<R, T>[] completedArcs = arcMan.getAndClearCompletedArcs();
			for (ActiveArc<R, T> completedArc : completedArcs) {
				Key<R, T> newKey = new Key<R, T>(completedArc, null);
				agenda.add(newKey);

				// do some debug output
				if (log.isLoggable(Level.INFO)) {
					StringBuilder builder = new StringBuilder("NEW KEY: PARTIAL PARSE ");
					for (ParseFormatter<R, T> formatter : ruleFactory.getDebugFormatters()) {
						for (Parse<R, T> p : newKey.getPartialParses(formatter)) {
							builder.append(" -> " + p.toString());
						}
					}
					log.info(builder.toString());
				}
			}

			// step 7 -- add the key to the chart now that we've updated the
			// state of all arcs
			chart.addKey(key);

			// TODO: In synchronous parsing, keys are not of the correct length

			// step 8 -- check for completed parses
			if (grammar.isStartSymbol(key.getLhs()) && key.getStartIndex() == 0
					&& key.getEndIndex() == input.length) {
				chart.addParse(key);
			}

			key.stopTimer();
		} while (!agenda.isEmpty() || i < input.length);

		// we have now evaluated the CFG backbone
		// if we got a full parse tree, only evaluate constraints for that
		// otherwise, evaluate constraints for the whole chart
	}

	private Key<R, T> makeDummyKey(T[] input, int i) {

		R dummyRule = ruleFactory.makeDummyRule(input[i]);
		ActiveArc<R, T> dummyArc = new ActiveArc<R, T>(i, i + 1, 1, dummyRule);
		Key<R, T> dummyKey = new Key<R, T>(dummyArc, input[i]);
		return dummyKey;
	}
}
