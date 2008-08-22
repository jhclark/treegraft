package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.util.FormatUtils;

/**
 * A ParseFormatter for a CFGRule that allows outputting parses as a source
 * tree, target tree, or target string.
 */
public class BasicTreeFormatter<T extends Token> implements TreeFormatter<T> {

	private final TokenFactory<T> tokenFactory;
	private final boolean showTree;
	private final boolean showScores;

	public BasicTreeFormatter(TokenFactory<T> tokenFactory, boolean showTree, boolean showScores) {
		this.tokenFactory = tokenFactory;
		this.showTree = showTree;
		this.showScores = showScores;
	}

	public String formatNonterminalAfter(T nodeLabel, FeatureScores scores) {
		if (showTree) {
			return ") ";
		} else {
			return "";
		}
	}

	public String formatNonterminalBefore(T nodeLabel, FeatureScores scores) {

		// open parentheses
		if (showTree) {
			if (showScores) {
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
		return tokenFactory.getTokenAsString(token) + " ";
	}
}
