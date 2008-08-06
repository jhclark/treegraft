package taru;

import java.io.File;

import taruDecoder.Scorer;

import hyperGraph.HGUtils;
import hyperGraph.HyperGraph;
import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.chartparser.ChartParser;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
import info.jonclark.treegraft.core.rules.SyncCFGRuleFactory;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.util.StringUtils;

public class TaruTreeGraftDriver {

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("Usage: program <arg>");
			System.exit(1);
		}

		TokenFactory<StringToken> tokenFactory = new StringTokenFactory();
		SyncCFGRuleFactory<StringToken> ruleFactory =
				new SyncCFGRuleFactory<StringToken>(tokenFactory);

		String input = "blah blah blah";
		ChartParser<SyncCFGRule<StringToken>, StringToken> parse =
				ChartParser.getSyncChartParser(new File("data/nlp_lab_sync.txt"), ruleFactory,
						tokenFactory);
		Chart<SyncCFGRule<StringToken>, StringToken> chart =
				new Chart<SyncCFGRule<StringToken>, StringToken>();
		parse.parse(tokenFactory.makeTerminalTokens(StringUtils.tokenize(input)), chart);

		TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken> graphBuilder =
				new TaruHypergraphBuilder<SyncCFGRule<StringToken>, StringToken>(tokenFactory);

		HyperGraph targetHG = chart.getParseForest(graphBuilder);

		HyperGraph binHG = HGUtils.binarizeHG(targetHG);
		final int K_BEST = 10;

		// setup the hg in the scorer

		// TODO: Unhard-code the feature model path and pass as parameter
		// TODO: Unhard-code the language model path and pass as parameter
		Scorer.getScorer().setHyperGraph(binHG);

		// Extract kbest
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
