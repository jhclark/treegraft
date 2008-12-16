package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.util.FormatUtils;

/**
 * A ParseFormatter for a CFGRule that allows outputting parses as a source
 * tree, target tree, or target string.
 */
public class BasicTreeFormatter<T extends Token> implements TreeFormatter<T> {

	private final TokenFactory<T> tokenFactory;
	private final Scorer<?, T> scorer;
	private final boolean showTree;
	private final boolean showScores;
	private final boolean showAllScores;

	public BasicTreeFormatter(TokenFactory<T> tokenFactory, boolean showTree, boolean showScores) {
		this.tokenFactory = tokenFactory;
		this.scorer = null;
		this.showTree = showTree;
		this.showScores = showScores;
		this.showAllScores = false;
	}

	public BasicTreeFormatter(TokenFactory<T> tokenFactory, boolean showTree, boolean showScores,
			Scorer<?, T> scorer, boolean showAllScores) {
		this.tokenFactory = tokenFactory;
		this.scorer = scorer;
		this.showTree = showTree;
		this.showScores = showScores;
		this.showAllScores = showAllScores;
	}

	public String formatNonterminalAfter(T nodeLabel, FeatureScores scores) {
		if (showTree) {
			return " )";
		} else {
			return "";
		}
	}

	// TODO: Store pointer to rule that created this node
	public String formatNonterminalBefore(T nodeLabel, FeatureScores scores) {

		// open parentheses
		if (showTree) {
			if (showAllScores) {
				StringBuilder builder = new StringBuilder();
				builder.append("(" + tokenFactory.getTokenAsString(nodeLabel) + " "
						+ FormatUtils.formatDouble4(scores.getLogProb()) + " = ");

				double[] probs = scores.getFeatureLogProbVector();
				double[] weights = scorer.getFeatureWeightVector();
				String[] labels = scorer.getFeatureProbVectorLabels();
				assert probs.length == labels.length;
				for (int i = 0; i < probs.length; i++) {
					builder.append(
//							labels[i] + "=" +
							FormatUtils.formatDouble2(probs[i] * weights[i]) +
							"(" + FormatUtils.formatDouble2(probs[i]) + ") ");
				}

				return builder.toString();
			} else if (showScores) {
				return "(" + tokenFactory.getTokenAsString(nodeLabel) + "="
						+ FormatUtils.formatDoubleExp(scores.getLogProb()) + " ";
			} else {
				return "(" + tokenFactory.getTokenAsString(nodeLabel) + " ";
			}
		} else {
			return "";
		}
	}

	public String formatTerminal(T token) {
		return tokenFactory.getTokenAsString(token);
	}
}
