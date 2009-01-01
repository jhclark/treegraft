package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;

public class LatticeArc<T extends Token> {

	private int startIndex;
	private int endIndex;
	private PartialParse<T> parse;

	public LatticeArc(int startIndex, int endIndex, PartialParse<T> parse) {
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

	public PartialParse<T> getParse() {
		return parse;
	}

	public String toString(TreeFormatter<T> formatter) {
		return "(" + startIndex + " " + endIndex + " " + parse.getSourceTree().toString(formatter)
				+ " " + parse.getTargetTree().toString(formatter) + ")";
	}
}
