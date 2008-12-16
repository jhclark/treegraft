package info.jonclark.treegraft.core.output;

import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.BasicTreeFormatter;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.util.FormatUtils;
import info.jonclark.util.StringUtils;

import java.io.PrintWriter;
import java.util.List;

public class BasicHypothesisFormatter<R extends GrammarRule<T>, T extends Token> implements
		HypothesisFormatter<R, T> {

	private final Scorer<R, T> scorer;
	private final TokenFactory<T> tokenFactory;

	public BasicHypothesisFormatter(TokenFactory<T> tokenFactory, Scorer<R, T> scorer) {
		this.scorer = scorer;
		this.tokenFactory = tokenFactory;
	}

	public void formatHypothesis(Result<R, T> result, PrintWriter out) {

		out.println("SrcSent " + result.nSentence + " " + result.inputSentence);
		for (int i = 0; i < result.nBestList.size(); i++) {

			// TODO: Show where phrase boundaries are (and output trees?
			// --
			// including recombined ambiguities)

			TreeFormatter<T> formatter =
					new BasicTreeFormatter<T>(tokenFactory, true, true, scorer, true);

			DecoderHypothesis<T> hyp = result.nBestList.get(i);
			List<T> hypTokens = hyp.getTokens();
			// remove <s> and </s>
			assert hypTokens.size() >= 2;
			hypTokens = hypTokens.subList(1, hypTokens.size() - 1);
			String[] tokens = tokenFactory.getTokensAsStrings(hypTokens);
			out.println(result.nSentence + " " + i + " " + StringUtils.untokenize(tokens));

			// Overall: -7826.12, Prob: -411.055, Rules: -29.8147,
			// RuleSGT: -18.981, RuleTGS: -29.8147, TransSGT: -98.6384,
			// TransTGS: -103.841, Frag: -0.332438, Length: -0.111402,
			// Words: 43,41
			// SGT -2.06084 TGS -1.48082

			// TODO: Display info about # words and true SGT and TGS
			out.print("Overall: " + FormatUtils.formatDouble4(hyp.getLogProb()));
			double[] logProbs = hyp.getScores().getFeatureLogProbVector();
			double[] weights = scorer.getFeatureWeightVector();
			String[] featureNames = scorer.getFeatureProbVectorLabels();
			for (int j = 0; j < featureNames.length; j++) {
				out.print(", " + featureNames[j] + ": " + FormatUtils.formatDouble2(logProbs[j])
						+ "(" + FormatUtils.formatDouble2(logProbs[j] + weights[j]) + ")");
			}
			out.println();

			out.println("Target parses:");
			for (Parse<T> parse : hyp.getParses()) {
				out.println("( " + parse.getStartIndex() + " " + parse.getEndIndex() + " "
						+ parse.getTargetTree().toString(formatter) + ")");
			}
			out.println("Source parses:");
			for (Parse<T> parse : hyp.getParses()) {
				out.println("( " + parse.getStartIndex() + " " + parse.getEndIndex() + " "
						+ parse.getSourceTree().toString(formatter) + ")");
			}
			out.println();
		}
	}

}
