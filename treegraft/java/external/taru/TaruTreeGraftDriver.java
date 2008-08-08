package taru;

import hyperGraph.HGUtils;
import hyperGraph.HyperGraph;
import info.jonclark.log.LogUtils;
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

		if (args.length != 0) {
			System.err.println("Usage: program <arg>");
			System.exit(1);
		}

		Scorer.modelFile = "data/de-en/taru.model";
		LMFeatureFunction.lmFile = "data/de-en/wmt08-en.lm.bin";
		LMFeatureFunction.loadLM();

		LogUtils.logAll();

		TokenFactory<StringToken> tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		String input =
				"ie meine Rede so wohlwollend aufgenommen haben , und richte meinen Dank auch an diejenigen ,"
						+ " die mir liebenswürdig erweise vorgeworfen haben , dass ich diese Rede nicht schon früher gehalten hätte . ";

		// String input =
		// "Le domaine des services publics de base d' intérêt social , culturel et caritatif doivent être repris dans l' article"
		// +
		// " 87 afin que ces institutions importantes puissent bénéficier d' une protection durable ."
		// ;

		StringToken[] inputTokens = tokenFactory.makeTerminalTokens(StringUtils.tokenize(input));

		log.info("Filtering grammar to input sentence...");
		HashSet<StringToken> vocabulary = new HashSet<StringToken>(Arrays.asList(inputTokens));

		log.info("Loading grammar...");
		Grammar<SyncCFGRule<StringToken>, StringToken> grammar =
				SyncCFGGrammarLoader.loadSyncGrammar(new File("data/de-en/de-en.all"),
						tokenFactory, vocabulary);

		log.info("Parsing...");
		ChartParser<SyncCFGRule<StringToken>, StringToken> parse =
				new ChartParser<SyncCFGRule<StringToken>, StringToken>(ruleFactory, grammar);
		Chart<SyncCFGRule<StringToken>, StringToken> chart =
				new Chart<SyncCFGRule<StringToken>, StringToken>();
		parse.parse(inputTokens, chart);

		log.info("Creating target hypergraph...");
		TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken> graphBuilder =
				new TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken>(tokenFactory);
		HyperGraph targetHG = chart.getParseForest(graphBuilder);

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
