package info.jonclark.treegraft.chartparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import info.jonclark.treegraft.core.formatting.parses.Parse;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter.OutputType;
import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.monocfg.MonoCFGGrammarLoader;
import info.jonclark.treegraft.core.monocfg.MonoCFGRule;
import info.jonclark.treegraft.core.monocfg.MonoCFGRuleFactory;
import info.jonclark.treegraft.core.monocfg.MonoParseFormatter;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.scoring.BasicScorer;
import info.jonclark.treegraft.core.scoring.ParseScorer;
import info.jonclark.treegraft.core.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.core.synccfg.SyncCFGRule;
import info.jonclark.treegraft.core.synccfg.SyncCFGRuleFactory;
import info.jonclark.treegraft.core.synccfg.SyncParseFormatter;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ChartParserTest {

	public void testParseRuleFeatures() throws ParseException {

		String str = "NP VVFIN VVFIN \":\" \"\\\"\" NN \",\" XY PUNCT";

		StringTokenFactory tokenFactory = new StringTokenFactory();
		StringToken[] toks = SyncCFGGrammarLoader.tokenizeRhs(str, tokenFactory);
		for (StringToken tok : toks)
			System.out.println(tok);
	}

	public void testLoadHugeGrammar() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();

		String[] files =
				new String[] { "grammar-t2t-1.gra", "lexicon-t2t-1.lex", "phrases-t2t-1.phr",
						"grammar-t2t-2.gra", "lexicon-t2t-2.lex", "phrases-t2t-2.phr" };
		for (String file : files) {
			System.out.println("Reading file: " + file);
			SyncCFGGrammarLoader.loadSyncGrammar(new File("data/huge/" + file), tokenFactory, null);
		}
	}

	public void testMonoChartParser() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		MonoCFGRuleFactory<StringToken> ruleFactory =
				new MonoCFGRuleFactory<StringToken>(tokenFactory);

		Grammar<MonoCFGRule<StringToken>, StringToken> grammar =
				MonoCFGGrammarLoader.loadMonoGrammar(new File("data/test.gra"), tokenFactory);
		ChartParser<MonoCFGRule<StringToken>, StringToken> parser =
				new ChartParser<MonoCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

		Chart<MonoCFGRule<StringToken>, StringToken> c =
				new Chart<MonoCFGRule<StringToken>, StringToken>();
		parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize("dogs bark")), c);

		MonoParseFormatter<StringToken> formatter =
				new MonoParseFormatter<StringToken>(tokenFactory);
		Parse<MonoCFGRule<StringToken>, StringToken>[] parses = c.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N dogs))(VP (V bark)))", parses[0].toString());
	}

	public void testNLPLabParsesMono() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		MonoCFGRuleFactory<StringToken> ruleFactory =
				new MonoCFGRuleFactory<StringToken>(tokenFactory);

		Grammar<MonoCFGRule<StringToken>, StringToken> grammar =
				MonoCFGGrammarLoader.loadMonoGrammar(new File("data/nlp_lab_sync.txt"),
						tokenFactory);
		ChartParser<MonoCFGRule<StringToken>, StringToken> parser =
				new ChartParser<MonoCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

		// 1) Load input sentences
		// 2) Load expected chart items

		String[] inputs =
				StringUtils.tokenize(
						FileUtils.getFileAsString(new File("data/nlp_lab_expected.in")).toLowerCase(),
						"\n");
		String[] expectedOutputs =
				StringUtils.tokenize(
						FileUtils.getFileAsString(new File("data/nlp_lab_expected.out")), "\n");

		assert inputs.length == expectedOutputs.length : "Input/output file line count mismatch";

		for (int i = 0; i < inputs.length; i++) {

			String input = inputs[i];
			List<String> expectedKeys =
					StringUtils.tokenizeBetweenMatching(expectedOutputs[i], "(", ")");

			System.out.println("Testing input: " + input);

			// parse the input
			Chart<MonoCFGRule<StringToken>, StringToken> c =
					new Chart<MonoCFGRule<StringToken>, StringToken>();
			parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize(input)), c);

			MonoParseFormatter<StringToken> formatter =
					new MonoParseFormatter<StringToken>(tokenFactory);
			Parse<MonoCFGRule<StringToken>, StringToken>[] parses =
					c.getGrammaticalParses(formatter);
			System.out.println("Found " + parses.length + " parses.");
			for (final Parse<MonoCFGRule<StringToken>, StringToken> x : parses) {
				System.out.println(x.toString());
			}

			// now see if the resulting chart looks right
			List<Key<MonoCFGRule<StringToken>, StringToken>> keys = c.getKeys();

			System.out.println("Got " + keys.size() + " keys");

			for (String expectedKey : expectedKeys) {
				checkSourceKey(keys, expectedKey);
			}

			// TODO: read each output and make sure it exists in the chart
		}
	}

	@Test
	public void testNLPLabParsesSync() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				SyncCFGGrammarLoader.loadSyncGrammar(new File("data/nlp_lab_sync.txt"),
						tokenFactory, null);
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

		String[] lines =
				StringUtils.tokenize(FileUtils.getFileAsString(new File(
						"data/nlp_lab_expected_sync.test")), "\n");

		for (int i = 0; i < lines.length; i++) {

			if (lines[i].equals("<TEST>")) {
				System.out.println("----------------------------------------");
				i++;
				String input = lines[i];
				i++;

				ArrayList<String> expectedKeys = new ArrayList<String>();
				while (lines[i].equals("<TEST>") == false) {
					expectedKeys.add(lines[i]);
					i++;
				}

				// backup and read the <TEST> at the next outer iteration
				i--;

				testSyncInput(input, expectedKeys, parser, tokenFactory);
			} // end if line.equals("<TEST>")
		} // end for each input
	}

	/**
	 * Tests a single synchronous input sequence and its expected keys
	 * 
	 * @param <T>
	 * @param input
	 * @param expectedKeys
	 * @param parser
	 * @param tokenFactory
	 * @throws ParseException
	 */
	private <T extends Token> void testSyncInput(String input, List<String> expectedKeys,
			ChartParser<SyncCFGRule<T>, T> parser, TokenFactory<T> tokenFactory)
			throws ParseException {

		input = input.toLowerCase();
		System.out.println("Testing input: " + input);

		// parse the input
		Chart<SyncCFGRule<T>, T> c = new Chart<SyncCFGRule<T>, T>();
		parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize(input)), c);

		showSyncParses(tokenFactory, c);

		ParseScorer<SyncCFGRule<T>, T> scorer = new BasicScorer<SyncCFGRule<T>, T>();
		SyncParseFormatter<T> targetFormatter =
				new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, scorer, true);

		// now see if the resulting chart looks right
		List<Key<SyncCFGRule<T>, T>> keys = c.getKeys();
		System.out.println("Got " + keys.size() + " keys");
		for (Key<SyncCFGRule<T>, T> key : keys) {
			System.out.print("Actual key: " + key.toString() + " -> ");
			List<Parse<SyncCFGRule<T>, T>> partialParses = key.getPartialParses(targetFormatter);
			for (Parse<SyncCFGRule<T>, T> parse : partialParses) {
				System.out.print(parse.toString() + "; ");
			}
			System.out.println();
		}

		// TODO: Check dual-containment: make sure all generated parses
		// were expected
		// TODO: read each output and make sure it exists in the chart

		// check transduced parses for each

		for (String expectedKey : expectedKeys) {

			String expectedSourceKey = StringUtils.substringBefore(expectedKey, " -> ");
			expectedSourceKey = StringUtils.substringBetweenMatching(expectedSourceKey, "(", ")");
			String expectedTargetKey = StringUtils.substringAfter(expectedKey, " -> ");

			List<Key<SyncCFGRule<T>, T>> matchingSourceKeys =
					checkSourceKey(keys, expectedSourceKey);
			checkTargetKey(matchingSourceKeys, expectedTargetKey, tokenFactory);
		}

		assertTrue("Got " + keys.size() + " keys instead of the expected " + expectedKeys.size()
				+ " for input \"" + input + "\"", keys.size() == expectedKeys.size());
	}

	private <T extends Token> void checkTargetKey(List<Key<SyncCFGRule<T>, T>> matchingSourceKeys,
			String expectedTargetKey, TokenFactory<T> tokenFactory) throws ParseException {

		ParseScorer<SyncCFGRule<T>, T> scorer = new BasicScorer<SyncCFGRule<T>, T>();
		SyncParseFormatter<T> formatter =
				new SyncParseFormatter<T>(tokenFactory, OutputType.TARGET_TREE, scorer, true);

		System.out.println("Checking for target parse: " + expectedTargetKey);

		boolean found = false;
		for (Key<SyncCFGRule<T>, T> matchingSourceKey : matchingSourceKeys) {

			List<Parse<SyncCFGRule<T>, T>> partialParses =
					matchingSourceKey.getPartialParses(formatter);

			// show actual partial parses
			// for (Parse<SyncCFGRule<T>, T> parse : partialParses) {
			// System.out.println("Actual partial parse: " + parse.toString());
			// }

			// do inefficient search over all parses
			expectedTargetKey = expectedTargetKey.trim();

			for (Parse<SyncCFGRule<T>, T> parse : partialParses) {
				if (expectedTargetKey.equals(parse.toString())) {
					found = true;
					break;
				}
			}
		}
		assertTrue("For key " + matchingSourceKeys.get(0) + " -- expected parse not found: "
				+ expectedTargetKey, found);

		// assertTrue("Got " + partialParses.length +
		// " partial parses instead of the expected "
		// + expectedTargetKeys.length, partialParses.length ==
		// expectedTargetKeys.length);
	}

	/**
	 * Tests a single source-side key to make sure it's what's expected
	 * 
	 * @param <R>
	 * @param <T>
	 * @param keys
	 * @param sourceKey
	 * @return the matching key
	 * @throws ParseException
	 */
	private <R extends GrammarRule<T>, T extends Token> List<Key<R, T>> checkSourceKey(
			List<Key<R, T>> keys, String sourceKey) throws ParseException {

		ArrayList<String> toks =
				StringUtils.tokenizeQuotedValues(sourceKey, " ", "(", ")", false, false,
						Integer.MAX_VALUE);

		int start = Integer.parseInt(toks.get(0)) - 1;
		int end = Integer.parseInt(toks.get(1)) - 1;

		String pos =
				StringTokenFactory.NON_TERMINAL_PREFIX + toks.get(2)
						+ StringTokenFactory.NON_TERMINAL_SUFFIX;
		if (sourceKey.contains("(")) {
			int[] backpointers = StringUtils.toIntArray(StringUtils.tokenize(toks.get(3)));
		} else {
			String word = toks.get(3);
		}
		int id = Integer.parseInt(toks.get(4));

		System.out.println("Checking for source key " + pos + " from " + start + " to " + end);
		// do an inefficient search through all keys
		boolean found = false;
		ArrayList<Key<R, T>> matches = new ArrayList<Key<R, T>>();
		for (Key<R, T> key : keys) {

			if (key.getStartIndex() == start && key.getEndIndex() == end
					&& key.getLhs().equals(pos)) {

				found = true;
				matches.add(key);
			}
		}

		assertTrue("Key " + pos + " (" + start + ", " + end + ") not found", found);
		return matches;
	}

	/**
	 * Displays any synchronous parses that were found during a test
	 * 
	 * @param <T>
	 * @param tokenFactory
	 * @param c
	 */
	private <T extends Token> void showSyncParses(TokenFactory<T> tokenFactory,
			Chart<SyncCFGRule<T>, T> c) {

		// show parses found
		ParseScorer<SyncCFGRule<T>, T> scorer = new BasicScorer<SyncCFGRule<T>, T>();

		for (OutputType outputType : OutputType.values()) {
			SyncParseFormatter<T> formatter =
					new SyncParseFormatter<T>(tokenFactory, outputType, scorer, true);

			Parse<SyncCFGRule<T>, T>[] parses = c.getGrammaticalParses(formatter);
			System.out.println(parses.length + " parses found.");
			for (final Parse<SyncCFGRule<T>, T> x : parses) {
				System.out.println(outputType + ": " + x.toString());
			}
		}
	}

	public void testSyncChartParser() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		File file = new File("data/de-en.gra");
		// File file = new File("data/test_sync.gra");

		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				SyncCFGGrammarLoader.loadSyncGrammar(new File("data/nlp_lab_sync.txt"),
						tokenFactory, null);
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

		Chart<SyncCFGRule<StringToken>, StringToken> chart =
				new Chart<SyncCFGRule<StringToken>, StringToken>();

		parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize("the dogs bark")), chart);

		ParseScorer<SyncCFGRule<StringToken>, StringToken> scorer =
				new BasicScorer<SyncCFGRule<StringToken>, StringToken>();
		SyncParseFormatter<StringToken> formatter;
		Parse<SyncCFGRule<StringToken>, StringToken>[] parses;

		for (OutputType outputType : OutputType.values()) {
			formatter = new SyncParseFormatter<StringToken>(tokenFactory, outputType, scorer, true);

			parses = chart.getGrammaticalParses(formatter);
			System.out.println(parses.length + " parses found.");
			for (final Parse<SyncCFGRule<StringToken>, StringToken> x : parses) {
				System.out.println(outputType + ": " + x.toString());
			}
		}

		formatter =
				new SyncParseFormatter<StringToken>(tokenFactory, OutputType.SOURCE_TREE, scorer,
						true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N the dogs))(VP (V bark)))", parses[0].toString());

		formatter =
				new SyncParseFormatter<StringToken>(tokenFactory, OutputType.TARGET_TREE, scorer,
						true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N perros))(VP_ES (V come)))", parses[0].toString());

		formatter =
				new SyncParseFormatter<StringToken>(tokenFactory, OutputType.TARGET_STRING, scorer,
						true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("perros come", parses[0].toString());
	}
}
