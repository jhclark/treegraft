package info.jonclark.treegraft.chartparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import info.jonclark.treegraft.core.mergingX.CrossProductParsePruner;
import info.jonclark.treegraft.core.recombination.NullRecombiner;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.LogLinearScorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.ChartParser;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGGrammarLoader;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRule;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRuleFactory;
import info.jonclark.treegraft.parsing.monocfg.MonoCFGRuleTransducer;
import info.jonclark.treegraft.parsing.parses.BasicTreeFormatter;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleTransducer;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class ChartParserTest {

	private <T extends Token> ForestUnpacker<SyncCFGRule<T>, T> getSyncUnpacker() {
		return new ForestUnpacker<SyncCFGRule<T>, T>(new LogLinearScorer<SyncCFGRule<T>, T>(new Feature[] {uniDirRuleFeature}, tokenFactory),
				new CrossProductParsePruner<SyncCFGRule<T>, T>(),
				new NullRecombiner<SyncCFGRule<T>, T>(), new SyncCFGRuleTransducer<T>());
	}

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
			SyncCFGGrammarLoader.loadSyncGrammar(new File("data/huge/" + file), tokenFactory, null,
					null, null);
		}
	}

	private <T extends Token> String[] getParses(TokenFactory<T> tokenFactory,
			Chart<MonoCFGRule<T>, T> c) {

		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, false);

		ForestUnpacker<MonoCFGRule<T>, T> unpacker =
				new ForestUnpacker<MonoCFGRule<T>, T>(new LogLinearScorer<MonoCFGRule<T>, T>(),
						new CrossProductParsePruner<MonoCFGRule<T>, T>(),
						new NullRecombiner<MonoCFGRule<T>, T>(), new MonoCFGRuleTransducer<T>());

		Parse<T>[] parses = c.getGrammaticalParses(unpacker);
		String[] result = new String[parses.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = parses[i].getTargetTree().toString(formatter);
		}
		return result;
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
				parser.parse(tokenFactory.makeTokens(StringUtils.tokenize("dogs bark"), true));

		String[] parses = getParses(tokenFactory, c);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N dogs))(VP (V bark)))", parses[0]);
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
					parser.parse(tokenFactory.makeTokens(StringUtils.tokenize(input), true));

			String[] parses = getParses(tokenFactory, c);
			System.out.println("Found " + parses.length + " parses.");
			for (String parse : parses) {
				System.out.println(parse);
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
						tokenFactory, null, null, null);
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
	 */
	private <T extends Token> void testSyncInput(String input, List<String> expectedKeys,
			ChartParser<SyncCFGRule<T>, T> parser, TokenFactory<T> tokenFactory)
			throws ParseException {

		input = input.toLowerCase();
		System.out.println("Testing input: " + input);

		// parse the input
		Chart<SyncCFGRule<T>, T> c =
				parser.parse(tokenFactory.makeTokens(StringUtils.tokenize(input), true));

		showSyncParses(tokenFactory, c);

		ForestUnpacker<SyncCFGRule<T>, T> unpacker = getSyncUnpacker();
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
			checkTargetKey(matchingSourceKeys, expectedTargetKeys, tokenFactory);
		}

		assertTrue("Got " + keys.size() + " keys instead of the expected " + expectedKeys.size()
				+ " for input \"" + input + "\"", keys.size() == expectedKeys.size());
	}

	private <T extends Token> void checkTargetKey(List<Key<SyncCFGRule<T>, T>> matchingSourceKeys,
			String[] expectedTargetKeys, TokenFactory<T> tokenFactory) throws ParseException {

		ForestUnpacker<SyncCFGRule<T>, T> unpacker = getSyncUnpacker();
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
					System.out.println(actualParse);
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

		BasicTreeFormatter<T> formatter = new BasicTreeFormatter<T>(tokenFactory, true, false);
		ForestUnpacker<SyncCFGRule<T>, T> unpacker =
				new ForestUnpacker<SyncCFGRule<T>, T>(new LogLinearScorer<SyncCFGRule<T>, T>(),
						new CrossProductParsePruner<SyncCFGRule<T>, T>(),
						new NullRecombiner<SyncCFGRule<T>, T>(), new SyncCFGRuleTransducer<T>());

		Parse<T>[] parses = c.getGrammaticalParses(unpacker);
		System.out.println(parses.length + " parses found.");
		for (int i = 0; i < parses.length; i++) {
			String src = parses[i].getSourceTree().toString(formatter);
			String tgt = parses[i].getTargetTree().toString(formatter);
			System.out.println(src + " -> " + tgt);
		}
	}
}
