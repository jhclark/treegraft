package taru;

import hyperGraph.HGUtils;
import hyperGraph.HyperGraph;
import info.jonclark.log.LogUtils;
import info.jonclark.properties.SmartProperties;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.chartparser.ChartParser;
import info.jonclark.treegraft.core.grammar.Grammar;
import info.jonclark.treegraft.core.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.core.synccfg.SyncCFGRule;
import info.jonclark.treegraft.core.synccfg.SyncCFGRuleFactory;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

import taruDecoder.Scorer;
import taruDecoder.features.LMFeatureFunction;

public class TaruTreeGraftDriver {

	private static final Logger log = LogUtils.getLogger();

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("Usage: program <props_file>");
			System.exit(1);
		}

		SmartProperties props = new SmartProperties(args[0]);
		File grammarFile = props.getPropertyFile("paths.grammarFile");
		String input = props.getPropertyString("input");
		Scorer.modelFile = props.getPropertyString("scorer.modelFile");
		LMFeatureFunction.lmFile = props.getPropertyString("lm.modelFile");

		TokenFactory<StringToken> tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		StringToken[] inputTokens = tokenFactory.makeTerminalTokens(StringUtils.tokenize(input));

		log.info("Filtering grammar to input sentence...");
		HashSet<StringToken> vocabulary = new HashSet<StringToken>(Arrays.asList(inputTokens));

		log.info("Loading grammar...");
		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				SyncCFGGrammarLoader.loadSyncGrammar(grammarFile, tokenFactory, vocabulary);

		log.info("Parsing...");
		ProfilerTimer timer = ProfilerTimer.newTimer("Taru-Treegraft", null, true, true);
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar, timer);
		Chart<SyncCFGRule<StringToken>, StringToken> chart = parser.parse(inputTokens);
		timer.pause();
		System.out.println(timer.getTimingReport(true));

		log.info("Creating target hypergraph...");
		TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken> graphBuilder =
				new TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken>(tokenFactory);
		HyperGraph targetHG = chart.getParseForest(graphBuilder);

		LMFeatureFunction.loadLM();

		log.info("Binarizing hypergraph...");
		HyperGraph binHG = HGUtils.binarizeHG(targetHG);
		final int K_BEST = 10;

		// setup the hg in the scorer

		// TODO: Unhard-code the feature model path and pass as parameter
		// TODO: Unhard-code the language model path and pass as parameter
		Scorer.getScorer().setHyperGraph(binHG);

		// Extract kbest
		log.info("Decoding...");
		HGUtils.extractKBestHypothesis(binHG, K_BEST);

		String[] translations = new String[binHG.getVertex(0).availableKBest()];
		for (int i = 1; i <= binHG.getVertex(0).availableKBest(); i++) {
			translations[i] = HGUtils.extractOriginalParse(binHG, 0, i);
		}

		// output translations
		for (String translation : translations) {
			System.out.println(translation);
		}
	}
}
