package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.tokens.Token;

public interface TreeFormatter<T extends Token> {
	
	public String formatNonterminalBefore(T node, FeatureScores scores, int depth);
	
	public String formatNonterminalAfter(T node, FeatureScores scores, int depth);
	
	public String formatTerminal(T node, int depth);
}
