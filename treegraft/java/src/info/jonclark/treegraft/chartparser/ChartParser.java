package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.RuleFactory;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.List;
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

	private final ProfilerTimer parserTimer;
	private final ProfilerTimer lexicalLookup;
	private final ProfilerTimer lexicalArcExtending;
	private final ProfilerTimer lexicalArcAdding;
	private final ProfilerTimer nonterminalLookup;
	private final ProfilerTimer nonterminalArcCreation;
	private final ProfilerTimer nonterminalArcExtending;
	private final ProfilerTimer nonterminalArcAdding;

	private boolean updated = false;

	private class PostMortem extends Thread {
		private final Grammar<R, T> grammar;

		public PostMortem(Grammar<R, T> grammar) {
			this.grammar = grammar;
		}

		public void run() {
			ArrayList<R> slowRules = grammar.getNMostProductiveRules(10);
			System.out.println("TOP " + slowRules.size() + " MOST PRODUCTIVE RULES: ");
			for (int i = 0; i < slowRules.size(); i++) {
				R rule = slowRules.get(i);
				System.out.println((i + 1) + ": " + rule.toString() + " = " + rule.getKeysCreated()
						+ " keys created directly or indirectly");
			}
		}
	}

	public ChartParser(RuleFactory<R, T> ruleFactory, Grammar<R, T> grammar) {
		this(ruleFactory, grammar, null);
	}

	public ChartParser(RuleFactory<R, T> ruleFactory, Grammar<R, T> grammar,
			ProfilerTimer parentTimer) {
		this.ruleFactory = ruleFactory;
		this.grammar = grammar;
		this.parserTimer = ProfilerTimer.newTimer("ChartParser", parentTimer, true, false);
		this.lexicalLookup = ProfilerTimer.newTimer("lexicalLookup", parserTimer, true, false);
		this.lexicalArcExtending =
				ProfilerTimer.newTimer("lexicalArcExtending", parserTimer, true, false);
		this.lexicalArcAdding =
				ProfilerTimer.newTimer("lexicalArcAdding", parserTimer, true, false);
		this.nonterminalLookup =
				ProfilerTimer.newTimer("nonterminalLookup", parserTimer, true, false);
		this.nonterminalArcCreation =
				ProfilerTimer.newTimer("nonterminalArcCreation", parserTimer, true, false);
		this.nonterminalArcExtending =
				ProfilerTimer.newTimer("nonterminalArcExtending", parserTimer, true, false);
		this.nonterminalArcAdding =
				ProfilerTimer.newTimer("nonterminalArcAdding", parserTimer, true, false);
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
	public Chart<R, T> parse(T[] input) {

		parserTimer.go();

		Chart<R, T> chart = new Chart<R, T>(input.length);
		Agenda<R, T> agenda = new Agenda<R, T>(chart);
		ActiveArcManager<R, T> arcMan = new ActiveArcManager<R, T>(input.length, parserTimer);
		// ConstraintEngine constraintEngine = new ConstraintEngine();

		// do a post-mortem analysis of what was taking so @#$! long if the user
		// kills the process with Ctrl+C or it otherwise dies unexpectedly
		PostMortem postmortem = new PostMortem(grammar);
		Runtime.getRuntime().addShutdownHook(postmortem);

		// step 1
		int i = 0;

		do {

			// step 2 -- turn tokens into keys and/or active arcs
			while (agenda.isEmpty() && i < input.length) {

				Key<R, T> dummyTerminalKey = makeDummyKey(input[i], i);

				lexicalLookup.go();
				List<R> matchingLexicalRules = grammar.getTerminalInitialRules(input[i]);
				for (final R lexicalRule : matchingLexicalRules) {

					// make a new arc due to a lexical input
					arcMan.createTerminalArc(i, lexicalRule, dummyTerminalKey);
				}
				lexicalLookup.pause();

				// extend terminals in arcs created by previous tokens
				// NOTE: Arcs we just added are extended by default
				lexicalArcExtending.go();
				arcMan.extendArcs(dummyTerminalKey);
				lexicalArcExtending.pause();

				// check for completed lexical arcs
				lexicalArcAdding.go();
				ActiveArc<R, T>[] completedArcs = arcMan.getAndClearCompletedArcs();
				for (final ActiveArc<R, T> completedArc : completedArcs) {
					agenda.addKeyToChartAndAgenda(completedArc);
				}
				lexicalArcAdding.pause();

				i++;
				log.info("Now processing input symbol " + i + " of " + input.length);
			}

			if (agenda.isEmpty()) {
				// we've extended everything we can
				break;
			}

			// step 3
			Key<R, T> key = agenda.get();
			key.startTimer();

			// check if this key begins a cycle, which we might want to break
			// final int DEPTH = 3;
			// if (key.formsCycle(DEPTH)) {
			// continue;
			// }

			// TODO: use timers for partially completed arcs too

			// step 4 -- add arcs beginning with this key
			nonterminalLookup.go();
			List<R> rules = grammar.getRulesStartingWith(key);
			nonterminalLookup.pause();
			nonterminalArcCreation.go();
			for (final R rule : rules) {
				arcMan.createNonterminalArc(key, rule);
			}
			nonterminalArcCreation.pause();

			// step 5 -- extend all arcs using the current key
			nonterminalArcExtending.go();
			arcMan.extendArcs(key);
			nonterminalArcExtending.pause();

			// step 6 -- check for completed arcs
			nonterminalArcAdding.go();
			ActiveArc<R, T>[] completedArcs = arcMan.getAndClearCompletedArcs();
			for (ActiveArc<R, T> completedArc : completedArcs) {
				agenda.addKeyToChartAndAgenda(completedArc);

				// do some debug output
				// if (log.isLoggable(Level.FINE)) {
				// StringBuilder builder = new
				// StringBuilder("NEW KEY: PARTIAL PARSE ");
				// for (ParseFormatter<R, T> formatter :
				// ruleFactory.getDebugFormatters()) {
				// for (Parse<R, T> p : newKey.getPartialParses(formatter)) {
				// builder.append(" -> " + p.toString());
				// }
				// }
				// log.fine(builder.toString());
				// }
			}
			nonterminalArcAdding.pause();

			// step 7 -- add the key to the chart now that we've updated the
			// state of all arcs (this has already been handled when we added
			// the key to the Agenda so that we can better support key packing)

			// step 8 -- check for completed parses
			if (grammar.isStartSymbol(key.getLhs()) && key.getStartIndex() == 0
					&& key.getEndIndex() == input.length) {
				chart.addParse(key);
			}

			if (chart.getKeys().size() % 1000 == 0) {
				if (updated == false) {
					log.info("Created " + arcMan.size() + " active arcs and "
							+ chart.getKeys().size() + " keys so far...");
				}
				updated = true;
			} else {
				updated = false;
			}

			key.stopTimer();
		} while (!agenda.isEmpty() || i < input.length);

		parserTimer.pause();

		log.info("PARSING COMPLETE: Created " + arcMan.size() + " active arcs and "
				+ chart.getKeys().size() + " keys.");

		postmortem.run();
		Runtime.getRuntime().removeShutdownHook(postmortem);

		return chart;
	}

	private Key<R, T> makeDummyKey(T token, int i) {

		R dummyRule = ruleFactory.makeDummyRule(token);
		ActiveArc<R, T> dummyArc = new ActiveArc<R, T>(i, i + 1, 1, dummyRule);
		Key<R, T> dummyKey = new Key<R, T>(dummyArc, token);
		return dummyKey;
	}
}
