package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;

public class LatticeArc<T extends Token> {
	
	private int startIndex;
	private int endIndex;
	private Parse<T> parse;

	public LatticeArc(int startIndex, int endIndex, Parse<T> parse) {
		this.endIndex = endIndex;
		this.parse = parse;
		this.startIndex = startIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public Parse<T> getParse() {
		return parse;
	}

	public String toString(TreeFormatter<T> formatter) {
		return "(" + startIndex + " " + endIndex + " " + parse.getLogProb() + " "
				+ parse.getTargetTree().toString(formatter) + ")";
	}
}
