package info.jonclark.treegraft.core.output;

import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.util.StringUtils;

import java.io.PrintWriter;
import java.util.List;

public class OptimizeNBestHypothesisFormatter<R extends GrammarRule<T>, T extends Token> implements
		HypothesisFormatter<R, T> {

	private final TokenFactory<T> tokenFactory;
	private Scorer<R, T> scorer;

	public OptimizeNBestHypothesisFormatter(TokenFactory<T> tokenFactory, Scorer<R, T> scorer) {
		this.tokenFactory = tokenFactory;
		this.scorer = scorer;
	}

	public void formatHypothesis(Result<R, T> result, PrintWriter out) {
	
		for (PartialParse<T> hyp : result.nBestList) {

			// TODO: Move the responsibility of <s> and </s> removal somewhere
			// else
			
			List<T> hypTokens = hyp.getTargetTokens();
			// remove <s> and </s>
			assert hypTokens.size() >= 2;
			hypTokens = hypTokens.subList(1, hypTokens.size() - 1);

			String[] tokens = tokenFactory.getTokensAsStrings(hypTokens);
			for(int i=0; i<tokens.length; i++) {
				if(tokens[i].equals("{"))
					tokens[i] = "-LCB-";
				else if(tokens[i].equals("}"))
					tokens[i] = "-RCB-";
				else if(tokens[i].equals("@"))
					tokens[i] = "-AT-";
				else if(tokens[i].equals("#"))
					tokens[i] = "-HASH-";
			}
			out.println(StringUtils.untokenize(tokens));

			double[] logProbs = hyp.getScores().getFeatureLogProbVector();
			out.println(StringUtils.untokenize(logProbs, " "));
		}
		if(result.nBestList.size() == 0) {
			out.println("_");
			double[] dummy = new double[scorer.getFeatureProbVectorLabels().length];
			out.println(StringUtils.untokenize(dummy, " "));
		}
		out.println();
	}

}
