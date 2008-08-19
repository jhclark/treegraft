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

		// read config file
		SmartProperties props = new SmartProperties(args[0]);
		File grammarFile = props.getPropertyFile("paths.grammarFile");
		String input = props.getPropertyString("input");
		Scorer.modelFile = props.getPropertyString("scorer.modelFile");
		LMFeatureFunction.lmFile = props.getPropertyString("lm.modelFile");

		String[] filterLHS = props.getPropertyStringArray("grammar.filterRulesWithLHS");
		String[] filterRHSNonterms =
				props.getPropertyStringArray("grammar.filterRulesWithNonterminalRHS");
		String[] filterRHSTerms =
				props.getPropertyStringArray("grammar.filterRulesWithTerminalRHS");

		ProfilerTimer timer = ProfilerTimer.newTimer("Taru-Treegraft", null, true, true);

		// init factories
		TokenFactory<StringToken> tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		StringToken[] inputTokens = tokenFactory.makeTokens(StringUtils.tokenize(input), true);
		HashSet<StringToken> vocabulary = new HashSet<StringToken>(Arrays.asList(inputTokens));
		
		// set up arbitrary rule filters
		HashSet<StringToken> filterLHSToks =
				new HashSet<StringToken>(Arrays.asList(tokenFactory.makeTokens(filterLHS, false)));
		HashSet<StringToken> filterRHSToks =
				new HashSet<StringToken>(Arrays.asList(tokenFactory.makeTokens(filterRHSNonterms,
						false)));
		filterRHSToks.addAll(Arrays.asList(tokenFactory.makeTokens(filterRHSTerms, true)));

		log.info("Loading grammar...");
		ProfilerTimer loadTimer = ProfilerTimer.newTimer("Loading", timer, true, true);
		ProfilerTimer loadGrammarTimer =
				ProfilerTimer.newTimer("loadGrammar", loadTimer, true, true);
		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				SyncCFGGrammarLoader.loadSyncGrammar(grammarFile, tokenFactory, vocabulary, filterLHSToks, filterRHSToks);
		loadGrammarTimer.pause();
		loadTimer.pause();

		log.info("Parsing...");
		ChartParser<SyncCFGRule<StringToken>, StringToken> parser =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar, timer);
		Chart<SyncCFGRule<StringToken>, StringToken> chart = parser.parse(inputTokens);
		timer.pause();

		log.info("Creating target hypergraph...");
		ProfilerTimer targetTimer =
				ProfilerTimer.newTimer("buildTargetHypergraph", timer, true, true);
		TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken> graphBuilder =
				new TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken>(tokenFactory);
		HyperGraph targetHG = chart.getParseForest(graphBuilder);
		targetTimer.pause();

		ProfilerTimer loadLMTimer = ProfilerTimer.newTimer("loadLM", loadTimer, true, true);
		loadTimer.go();
		LMFeatureFunction.loadLM();
		loadLMTimer.pause();
		loadTimer.pause();

		ProfilerTimer decoderTimer = ProfilerTimer.newTimer("Decoder", timer, true, true);
		ProfilerTimer binarizeTimer =
				ProfilerTimer.newTimer("binarizeHypergraph", decoderTimer, true, true);
		log.info("Binarizing hypergraph...");
		HyperGraph binHG = HGUtils.binarizeHG(targetHG);
		binarizeTimer.pause();
		final int K_BEST = 10;

		// setup the hg in the scorer
		Scorer.getScorer().setHyperGraph(binHG);

		// Extract kbest
		log.info("Decoding...");
		ProfilerTimer extractTimer =
				ProfilerTimer.newTimer("extractKBest", decoderTimer, true, true);
		HGUtils.extractKBestHypothesis(binHG, K_BEST);
		extractTimer.pause();
		decoderTimer.pause();

		String[] translations = new String[binHG.getVertex(0).availableKBest()];
		for (int i = 1; i <= binHG.getVertex(0).availableKBest(); i++) {
			translations[i] = HGUtils.extractOriginalParse(binHG, 0, i);
		}

		log.info(timer.getTimingReport(true));

		// output translations
		for (String translation : translations) {
			log.info("TRANSLATION: " + translation);
		}
	}
}
