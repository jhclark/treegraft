package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.util.FormatUtils;
import info.jonclark.util.StringUtils;

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
	private final boolean prettyPrint;

	public BasicTreeFormatter(TokenFactory<T> tokenFactory, boolean showTree, boolean showScores) {
		this.tokenFactory = tokenFactory;
		this.scorer = null;
		this.showTree = showTree;
		this.showScores = showScores;
		this.showAllScores = false;
		this.prettyPrint = false;
	}

	public BasicTreeFormatter(TokenFactory<T> tokenFactory, boolean showTree, boolean showScores,
			Scorer<?, T> scorer, boolean showAllScores, boolean prettyPrint) {
		this.tokenFactory = tokenFactory;
		this.scorer = scorer;
		this.showTree = showTree;
		this.showScores = showScores;
		this.showAllScores = showAllScores;
		this.prettyPrint = prettyPrint;
	}

	public String formatNonterminalAfter(T nodeLabel, FeatureScores scores, int depth) {
		if (showTree) {
			if (prettyPrint) {
				return " )\n";
			} else {
				return " )";
			}
		} else {
			return "";
		}
	}

	// TODO: Store pointer to rule that created this node
	public String formatNonterminalBefore(T nodeLabel, FeatureScores scores, int depth) {

		// open parentheses
		if (showTree) {

			StringBuilder builder = new StringBuilder();
			if (prettyPrint) {
				builder.append(StringUtils.duplicateCharacter(' ', depth));
			}

			if (showAllScores) {
				builder.append("(" + nodeLabel.getWord() + " "
						+ FormatUtils.formatDouble4(scores.getLogProb()) + " = ");

				double[] probs = scores.getFeatureLogProbVector();
				double[] weights = scorer.getFeatureWeightVector();
				String[] labels = scorer.getFeatureProbVectorLabels();
				assert probs.length == labels.length;
				for (int i = 0; i < probs.length; i++) {
					builder.append(
					// labels[i] + "=" +
					FormatUtils.formatDouble2(probs[i] * weights[i]) + "("
							+ FormatUtils.formatDouble2(probs[i]) + ") ");
				}

			} else if (showScores) {
				builder.append("(" + nodeLabel.getWord() + "="
						+ FormatUtils.formatDoubleExp(scores.getLogProb()) + " ");
			} else {
				builder.append("(" + nodeLabel.getWord() + " ");
			}

			if (prettyPrint) {
				builder.append("\n");
			}
			return builder.toString();
		} else {
			return "";
		}
	}

	public String formatTerminal(T token, int depth) {
		StringBuilder builder = new StringBuilder();
		if (prettyPrint) {
			builder.append(StringUtils.duplicateCharacter(' ', depth));
		}
		builder.append(token.getWord());
		return builder.toString();
	}
}
