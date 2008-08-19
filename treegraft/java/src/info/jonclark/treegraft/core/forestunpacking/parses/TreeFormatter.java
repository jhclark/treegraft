package info.jonclark.treegraft.core.forestunpacking.parses;

import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;

public interface TreeFormatter<T extends Token> {
	
	public String formatNonterminalBefore(T node, Scores scores);
	
	public String formatNonterminalAfter(T node, Scores scores);
	
	public String formatTerminal(T node);
}
