package info.jonclark.treegraft.chartparser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import info.jonclark.treegraft.core.Grammar;
import info.jonclark.treegraft.core.Parse;
import info.jonclark.treegraft.core.formatting.parses.MonoParseFormatter;
import info.jonclark.treegraft.core.formatting.parses.SyncParseFormatter;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter.OutputType;
import info.jonclark.treegraft.core.rules.MonoCFGRule;
import info.jonclark.treegraft.core.rules.MonoCFGRuleFactory;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
import info.jonclark.treegraft.core.rules.SyncCFGRuleFactory;
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
		StringToken[] toks = Grammar.tokenizeRhs(str, tokenFactory);
		for (StringToken tok : toks)
			System.out.println(tok);
	}

	public void testLoadHugeGrammar() throws ParseException, IOException {

		StringTokenFactory tokenFactory = new StringTokenFactory();

		String[] files =
				new String[] { "grammar-t2t-1.gra", "lexicon-t2t-1.lex", "phrases-t2t-1.phr",
						"grammar-t2t-2.gra", "lexicon-t2t-2.lex", "phrases-t2t-2.phr" };
		for (String file : files) {
			System.out.println("Reading file: " + file);
			Grammar.loadSyncGrammar(new File("data/huge/" + file), tokenFactory);
		}
	}

	public void testMonoChartParser() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		MonoCFGRuleFactory<StringToken> ruleFactory =
				new MonoCFGRuleFactory<StringToken>(tokenFactory);

		Grammar<MonoCFGRule<StringToken>, StringToken> grammar =
				Grammar.loadMonoGrammar(new File("data/test.gra"), tokenFactory);
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
				Grammar.loadMonoGrammar(new File("data/nlp_lab_sync.txt"), tokenFactory);
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
				ArrayList<String> toks =
						StringUtils.tokenizeQuotedValues(expectedKey, " ", "(", ")", false, false,
								Integer.MAX_VALUE);

				int start = Integer.parseInt(toks.get(0)) - 1;
				int end = Integer.parseInt(toks.get(1)) - 1;

				String pos =
						StringTokenFactory.NON_TERMINAL_PREFIX + toks.get(2)
								+ StringTokenFactory.NON_TERMINAL_SUFFIX;
				if (expectedKey.contains("(")) {
					int[] backpointers = StringUtils.toIntArray(StringUtils.tokenize(toks.get(3)));
				} else {
					String word = toks.get(3);
				}
				int id = Integer.parseInt(toks.get(4));

				System.out.println("Checking for " + pos + " from " + start + " to " + end);

				// do an inefficient search through all keys
				boolean found = false;
				for (Key<MonoCFGRule<StringToken>, StringToken> key : keys) {

					if (key.getStartIndex() == start && key.getEndIndex() == end
							&& key.getLhs().equals(pos)) {

						found = true;
						break;
					}
				}
				assertTrue("Key " + pos + " (" + start + ", " + end + ") not found", found);
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
				Grammar.loadSyncGrammar(new File("data/nlp_lab_sync.txt"), tokenFactory);
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

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
			Chart<SyncCFGRule<StringToken>, StringToken> c =
					new Chart<SyncCFGRule<StringToken>, StringToken>();
			parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize(input)), c);

			// show parses found
			for (OutputType outputType : OutputType.values()) {
				SyncParseFormatter<StringToken> formatter =
						new SyncParseFormatter<StringToken>(tokenFactory, outputType, true);

				Parse<SyncCFGRule<StringToken>, StringToken>[] parses =
						c.getGrammaticalParses(formatter);
				System.out.println(parses.length + " parses found.");
				for (final Parse<SyncCFGRule<StringToken>, StringToken> x : parses) {
					System.out.println(outputType + ": " + x.toString());
				}
			}

			// now see if the resulting chart looks right
			List<Key<SyncCFGRule<StringToken>, StringToken>> keys = c.getKeys();

			System.out.println("Got " + keys.size() + " keys");

			for (String expectedKey : expectedKeys) {
				ArrayList<String> toks =
						StringUtils.tokenizeQuotedValues(expectedKey, " ", "(", ")", false, false,
								Integer.MAX_VALUE);

				int start = Integer.parseInt(toks.get(0)) - 1;
				int end = Integer.parseInt(toks.get(1)) - 1;

				String pos =
						StringTokenFactory.NON_TERMINAL_PREFIX + toks.get(2)
								+ StringTokenFactory.NON_TERMINAL_SUFFIX;
				if (expectedKey.contains("(")) {
					int[] backpointers = StringUtils.toIntArray(StringUtils.tokenize(toks.get(3)));
				} else {
					String word = toks.get(3);
				}
				int id = Integer.parseInt(toks.get(4));

				System.out.println("Checking for " + pos + " from " + start + " to " + end);

				// do an inefficient search through all keys
				boolean found = false;
				for (Key<SyncCFGRule<StringToken>, StringToken> key : keys) {

					if (key.getStartIndex() == start && key.getEndIndex() == end
							&& key.getLhs().equals(pos)) {

						found = true;
						break;
					}
				}
				assertTrue("Key " + pos + " (" + start + ", " + end + ") not found", found);
			}

			// TODO: read each output and make sure it exists in the chart
		}
	}

	public void testSyncChartParser() throws Exception {

		StringTokenFactory tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		File file = new File("data/de-en.gra");
		// File file = new File("data/test_sync.gra");

		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				Grammar.loadSyncGrammar(new File("data/nlp_lab_sync.txt"), tokenFactory);
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar);

		Chart<SyncCFGRule<StringToken>, StringToken> chart =
				new Chart<SyncCFGRule<StringToken>, StringToken>();

		parser.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize("the dogs bark")), chart);

		SyncParseFormatter<StringToken> formatter;
		Parse<SyncCFGRule<StringToken>, StringToken>[] parses;

		for (OutputType outputType : OutputType.values()) {
			formatter = new SyncParseFormatter<StringToken>(tokenFactory, outputType, true);

			parses = chart.getGrammaticalParses(formatter);
			System.out.println(parses.length + " parses found.");
			for (final Parse<SyncCFGRule<StringToken>, StringToken> x : parses) {
				System.out.println(outputType + ": " + x.toString());
			}
		}

		formatter = new SyncParseFormatter<StringToken>(tokenFactory, OutputType.SOURCE_TREE, true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N the dogs))(VP (V bark)))", parses[0].toString());

		formatter = new SyncParseFormatter<StringToken>(tokenFactory, OutputType.TARGET_TREE, true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("(S (NP (N perros))(VP_ES (V come)))", parses[0].toString());

		formatter =
				new SyncParseFormatter<StringToken>(tokenFactory, OutputType.TARGET_STRING, true);
		parses = chart.getGrammaticalParses(formatter);
		assertEquals(1, parses.length);
		assertEquals("perros come", parses[0].toString());
	}
}
