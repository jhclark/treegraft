package info.jonclark.treegraft.parsing.unification;

import info.jonclark.treegraft.parsing.rules.GrammarRule;

public class Constraint {

	// what it means...
	private String text; // fix this

	// trace elements
	private GrammarRule grammarRule;
	private String file;
	private int lineNumber;
	
	public Constraint(String text, String file, int lineNumber) {
		this.text = text;
		this.file = file;
		this.lineNumber = lineNumber;
	}

	public GrammarRule getGrammarRule() {
		return grammarRule;
	}

	public String getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

}
