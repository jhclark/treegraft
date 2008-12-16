package info.jonclark.treegraft.chartparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import info.jonclark.treegraft.core.featureimpl.RuleFeature;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.LogLinearScorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.ChartParser;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.merging.BeamSearchMerger;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGGrammarLoader;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRule;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRuleFactory;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRuleTransducer;
import info.jonclark.treegraft.parsing.oov.OutOfVocabularyHandler;
import info.jonclark.treegraft.parsing.oov.PanicOOVHandler;
import info.jonclark.treegraft.parsing.parses.BasicTreeFormatter;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleTransducer;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ChartParserTest {

	private <T extends Token> ForestUnpacker<SyncCFGRule<T>, T> getSyncUnpacker(
			TokenFactory<T> tokenFactory, List<T> inputTokens) {

		ParseFactory<SyncCFGRule<T>, T> parseFactory =
				new ParseFactory<SyncCFGRule<T>, T>(tokenFactory, inputTokens.size());
		BeamSearchMerger<SyncCFGRule<T>, T> merger =
				new BeamSearchMerger<SyncCFGRule<T>, T>(tokenFactory, parseFactory, 10000, false,
						false, null);

		return new ForestUnpacker<SyncCFGRule<T>, T>(new LogLinearScorer<SyncCFGRule<T>, T>(
				new Feature[] { new RuleFeature<SyncCFGRule<T>, T>(1.0, 1.0, null) }, tokenFactory,
				null), merger, parseFactory, new SyncCFGRuleTransducer<T>(), inputTokens);
	}

	private <T extends Token> ForestUnpacker<MonoCFGRule<T>, T> getMonoUnpacker(
			TokenFactory<T> tokenFactory, List<T> inputTokens) {

		ParseFactory<MonoCFGRule<T>, T> parseFactory =
				new ParseFactory<MonoCFGRule<T>, T>(tokenFactory, inputTokens.size());
		BeamSearchMerger<MonoCFGRule<T>, T> merger =
				new BeamSearchMerger<MonoCFGRule<T>, T>(tokenFactory, parseFactory, 10000, false,
						false, null);

		return new ForestUnpacker<MonoCFGRule<T>, T>(new LogLinearScorer<MonoCFGRule<T>, T>(
				new Feature[] { new RuleFeature<MonoCFGRule<T>, T>(1.0, 1.0, null) }, tokenFactory,
				null), merger, parseFactory, new MonoCFGRuleTransducer<T>(), inputTokens);
	}

	private <R extends GrammarRule<T>, T extends Token> OutOfVocabularyHandler<R, T> getOOVHandler(
			Grammar<R, T> grammar, TokenFactory<T> tokenFactory) {

		return new PanicOOVHandler<R, T>(null, tokenFactory);
	}

	private <T extends Token> Grammar<SyncCFGRule<T>, T> loadSyncGrammar(
			TokenFactory<T> tokenFactory, File f) throws IOException, ParseException,
			FileNotFoundException, RuleException {

		SyncCFGGrammarLoader<T> grammarLoader = new SyncCFGGrammarLoader<T>(tokenFactory, true);
		Grammar<SyncCFGRule<T>, T> grammar =
				new Grammar<SyncCFGRule<T>, T>(tokenFactory, Grammar.DEFAULT_START_SYMBOLS, null,
						null, null);
		grammarLoader.loadGrammar(grammar, new FileInputStream(f), f.getAbsolutePath(), "UTF8",
				null);
		return grammar;
	}

	private <T extends Token> Grammar<MonoCFGRule<T>, T> loadMonoGrammar(
			TokenFactory<T> tokenFactory, File f) throws IOException, ParseException,
			FileNotFoundException, RuleException {

		Grammar<MonoCFGRule<T>, T> grammar =
				new Grammar<MonoCFGRule<T>, T>(tokenFactory, Grammar.DEFAULT_START_SYMBOLS, null,
						null, null);
		MonoCFGGrammarLoader<T> loader = new MonoCFGGrammarLoader<T>(tokenFactory, true);
		loader.loadGrammar(grammar, new FileInputStream(f), f.getAbsolutePath(), "utf-8", null);
		return grammar;
	}

	private <T extends Token> String[] getParses(TokenFactory<T> tokenFactory,
			Chart<MonoCFGRule<T>, T> c, List<T> tokens) {

		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, false);

		ForestUnpacker<MonoCFGRule<T>, T> unpacker = getMonoUnpacker(tokenFactory, tokens);

		Parse<T>[] parses = c.getGrammaticalParses(unpacker);
		String[] result = new String[parses.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = parses[i].getTargetTree().toString(formatter);
		}
		return result;
	}

	@Test
	public void testMonoChartParser() throws Exception {
		testMonoChartParser(new StringTokenFactory());
		// testMonoChartParser(new IntegerTokenFactory());
	}

	private <T extends Token> void testMonoChartParser(TokenFactory<T> tokenFactory)
			throws Exception {

		MonoCFGRuleFactory<T> ruleFactory = new MonoCFGRuleFactory<T>(tokenFactory);

		Grammar<MonoCFGRule<T>, T> grammar =
				loadMonoGrammar(tokenFactory, new File("data/test.gra"));
		ChartParser<MonoCFGRule<T>, T> parser =
				new ChartParser<MonoCFGRule<T>, T>(ruleFactory, grammar, getOOVHandler(grammar,
						tokenFactory));

		T[] tokens = tokenFactory.makeTokens(StringUtils.tokenize("dogs bark"), true);
		Chart<MonoCFGRule<T>, T> c = parser.parse(tokens);

		String[] parses = getParses(tokenFactory, c, Arrays.asList(tokens));
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N dogs ) )(VP (V bark ) ) )", parses[0]);
	}

	@Test
	public void testNLPLabParsesMono() throws IOException, ParseException, RuleException {
		testNLPLabParsesMono(new StringTokenFactory());
		// testNLPLabParsesMono(new IntegerTokenFactory());
	}

	private <T extends Token> void testNLPLabParsesMono(TokenFactory<T> tokenFactory)
			throws IOException, ParseException, RuleException {

		MonoCFGRuleFactory<T> ruleFactory = new MonoCFGRuleFactory<T>(tokenFactory);

		Grammar<MonoCFGRule<T>, T> grammar =
				loadMonoGrammar(tokenFactory, new File("data/nlp_lab_test.txt"));
		ChartParser<MonoCFGRule<T>, T> parser =
				new ChartParser<MonoCFGRule<T>, T>(ruleFactory, grammar, getOOVHandler(grammar,
						tokenFactory));

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

			T[] tokens = tokenFactory.makeTokens(StringUtils.tokenize(input), true);
			// parse the input
			try {
				Chart<MonoCFGRule<T>, T> c = parser.parse(tokens);
				String[] parses = getParses(tokenFactory, c, Arrays.asList(tokens));
				System.out.println("Found " + parses.length + " parses.");
				for (String parse : parses) {
					System.out.println(parse);
				}

				// now see if the resulting chart looks right
				List<Key<MonoCFGRule<T>, T>> keys = c.getKeys();

				System.out.println("Got " + keys.size() + " keys");

				for (String expectedKey : expectedKeys) {
					checkSourceKey(keys, expectedKey);
				}
			} catch (Throwable t) {
				Assert.fail("Exception for input: " + input + "; " + StringUtils.getStackTrace(t));
			}

			// TODO: read each output and make sure it exists in the chart
		}
	}

	@Test
	public void testNLPLabParsesSync() throws Exception {
		testNLPLabParsesSync(new StringTokenFactory());
		// testNLPLabParsesSync(new IntegerTokenFactory());
	}

	private <T extends Token> void testNLPLabParsesSync(TokenFactory<T> tokenFactory)
			throws Exception {
		SyncCFGRuleFactory<T> ruleFactory = new SyncCFGRuleFactory<T>(tokenFactory);

		Grammar<SyncCFGRule<T>, T> grammar =
				loadSyncGrammar(tokenFactory, new File("data/nlp_lab_sync.txt"));

		ChartParser<SyncCFGRule<T>, T> parser =
				new ChartParser<SyncCFGRule<T>, T>(ruleFactory, grammar, getOOVHandler(grammar,
						tokenFactory));

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
				while (lines[i].equals("<TEST>") == false && lines[i].equals("</TESTS>") == false) {
					if (lines[i].startsWith(";") == false) {
						expectedKeys.add(lines[i]);
					}
					i++;
				}
				Collections.reverse(expectedKeys);

				// backup and read the <TEST> at the next outer iteration
				i--;

				testSyncInput(input, expectedKeys, parser, tokenFactory);
			} else if (lines[i].equals("</TESTS>")) {
				break;
			}
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
	 * @throws RuleException
	 */
	private <T extends Token> void testSyncInput(String input, List<String> expectedKeys,
			ChartParser<SyncCFGRule<T>, T> parser, TokenFactory<T> tokenFactory)
			throws ParseException, RuleException {

		input = input.toLowerCase();
		System.out.println("Testing input: " + input);
		T[] tokens = tokenFactory.makeTokens(StringUtils.tokenize(input), true);

		// parse the input
		Chart<SyncCFGRule<T>, T> c = parser.parse(tokens);

		showSyncParses(tokenFactory, c, Arrays.asList(tokens));

		ForestUnpacker<SyncCFGRule<T>, T> unpacker =
				getSyncUnpacker(tokenFactory, Arrays.asList(tokens));
		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, true);

		// now see if the resulting chart looks right
		List<Key<SyncCFGRule<T>, T>> keys = c.getKeys();
		System.out.println("Got " + keys.size() + " keys");
		for (Key<SyncCFGRule<T>, T> key : keys) {
			System.out.print("Actual key: " + key.toString() + " -> ");
			List<Parse<T>> partialParses = unpacker.getPartialParses(key);
			for (Parse<T> parse : partialParses) {
				String str = parse.getTargetTree().toString(formatter);
				System.out.print(str + "; ");
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
			String strExpectedTargetKeys = StringUtils.substringAfter(expectedKey, " -> ");
			String[] expectedTargetKeys = StringUtils.tokenize(strExpectedTargetKeys, ";");
			StringUtils.trimTokens(expectedTargetKeys);

			List<Key<SyncCFGRule<T>, T>> matchingSourceKeys =
					checkSourceKey(keys, expectedSourceKey);
			checkTargetKey(matchingSourceKeys, expectedTargetKeys, tokenFactory,
					Arrays.asList(tokens));
		}

		assertTrue("Got " + keys.size() + " keys instead of the expected " + expectedKeys.size()
				+ " for input \"" + input + "\"", keys.size() == expectedKeys.size());
	}

	private <T extends Token> void checkTargetKey(List<Key<SyncCFGRule<T>, T>> matchingSourceKeys,
			String[] expectedTargetKeys, TokenFactory<T> tokenFactory, List<T> tokens)
			throws ParseException {

		ForestUnpacker<SyncCFGRule<T>, T> unpacker = getSyncUnpacker(tokenFactory, tokens);
		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, true);

		for (String expectedTargetKey : expectedTargetKeys) {
			System.out.println("Checking for target parse: " + expectedTargetKey);

			boolean found = false;
			for (Key<SyncCFGRule<T>, T> matchingSourceKey : matchingSourceKeys) {

				List<Parse<T>> partialParses = unpacker.getPartialParses(matchingSourceKey);

				// show actual partial parses
				// for (Parse<SyncCFGRule<T>, T> parse : partialParses) {
				// System.out.println("Actual partial parse: " +
				// parse.toString());
				// }

				// do inefficient search over all parses
				expectedTargetKey = expectedTargetKey.trim();

				for (Parse<T> parse : partialParses) {
					String actualParse = parse.getTargetTree().toString(formatter);
					System.out.println("ACTUAL PARSE: " + actualParse);
					if (expectedTargetKey.equals(actualParse)) {
						found = true;
						assertTrue("Got " + partialParses.size()
								+ " partial parses instead of the expected "
								+ expectedTargetKeys.length + " for: " + expectedTargetKey,
								partialParses.size() == expectedTargetKeys.length);
						break;
					}
				}
			}
			assertTrue("For key " + matchingSourceKeys.get(0) + " -- expected parse not found: "
					+ expectedTargetKey, found);
		}
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

			System.out.println("ACTUAL KEY: " + key.toString());
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
			Chart<SyncCFGRule<T>, T> c, List<T> tokens) {

		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, false);
		ForestUnpacker<SyncCFGRule<T>, T> unpacker = getSyncUnpacker(tokenFactory, tokens);

		Parse<T>[] parses = c.getGrammaticalParses(unpacker);
		System.out.println(parses.length + " parses found.");
		for (int i = 0; i < parses.length; i++) {
			String src = parses[i].getSourceTree().toString(formatter);
			String tgt = parses[i].getTargetTree().toString(formatter);
			System.out.println(src + " -> " + tgt);
		}
	}
}
