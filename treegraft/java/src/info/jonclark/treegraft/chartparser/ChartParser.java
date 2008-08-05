package info.jonclark.treegraft.chartparser;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.Grammar;
import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.formatting.ParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.MonoCFGRule;
import info.jonclark.treegraft.core.rules.RuleFactory;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

/**
 * A left-to-right chart parser that accepts both monolingual and synchronous
 * grammars.
 */
public class ChartParser<R extends GrammarRule<T>, T extends Token> {

	private Grammar<R, T> grammar;
	private RuleFactory<R, T> ruleFactory;
	private static final Logger log = LogUtils.getLogger();

	private ChartParser(RuleFactory<R, T> ruleFactory) {
		this.ruleFactory = ruleFactory;
	}

	/**
	 * Create a new ChartParser for a monolingual grammar.
	 * 
	 * @param <T>
	 * @param grammarFile
	 * @param tokenFactory
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static <T extends Token> ChartParser<MonoCFGRule<T>, T> getMonoChartParser(
			File grammarFile, RuleFactory<MonoCFGRule<T>, T> ruleFactory,
			TokenFactory<T> tokenFactory) throws IOException, ParseException {

		ChartParser<MonoCFGRule<T>, T> parser = new ChartParser<MonoCFGRule<T>, T>(ruleFactory);
		parser.grammar = new Grammar<MonoCFGRule<T>, T>(tokenFactory);
		Grammar.loadMonoGrammar(grammarFile, tokenFactory, parser.grammar);
		return parser;
	}

	/**
	 * Create a new ChartParser for a synchronous grammar.
	 * 
	 * @param <T>
	 * @param grammarFile
	 * @param tokenFactory
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public static <T extends Token> ChartParser<SyncCFGRule<T>, T> getSyncChartParser(
			File grammarFile, RuleFactory<SyncCFGRule<T>, T> ruleFactory,
			TokenFactory<T> tokenFactory) throws IOException, ParseException {

		ChartParser<SyncCFGRule<T>, T> parser = new ChartParser<SyncCFGRule<T>, T>(ruleFactory);
		parser.grammar = new Grammar<SyncCFGRule<T>, T>(tokenFactory);
		Grammar.loadSyncGrammar(grammarFile, tokenFactory, parser.grammar);
		return parser;
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

			System.out.println("PROCESSING KEY: " + key + " from rule " + key.getRule().toString());

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

				System.out.print("NEW KEY: PARTIAL PARSE ");
				for (ParseFormatter<R, T> formatter : ruleFactory.getDebugFormatters()) {
					for (Parse<R, T> p : newKey.getPartialParses(formatter)) {
						System.out.print(" -> " + p.toString());
					}
				}
				System.out.println();
			}

			// step 7 -- add the key to the chart now that we've updated the
			// state of all arcs
			chart.add(key);

			// TODO: In synchronous parsing, keys are not of the correct length

			// step 8 (handled in Chart) -- check for completed parses
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
