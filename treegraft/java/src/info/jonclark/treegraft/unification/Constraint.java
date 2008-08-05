package info.jonclark.treegraft.unification;

import info.jonclark.treegraft.core.rules.GrammarRule;

import java.io.File;

public class Constraint {

	// what it means...
	private String text; // fix this

	// trace elements
	private GrammarRule grammarRule;
	private File file;
	private int lineNumber;
	
	public Constraint(String text, File file, int lineNumber) {
		this.text = text;
		this.file = file;
		this.lineNumber = lineNumber;
	}

	public GrammarRule getGrammarRule() {
		return grammarRule;
	}

	public File getFile() {
		return file;
	}

	public int getLineNumber() {
		return lineNumber;
	}

}
