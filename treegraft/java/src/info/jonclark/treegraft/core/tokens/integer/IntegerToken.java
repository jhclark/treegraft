package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.Token;

public class IntegerToken implements Token {

	protected final int id;
	protected final boolean terminal;

	protected IntegerToken(int id, boolean terminal) {
		this.id = id;
		this.terminal = terminal;
	}

	public String getId() {
		return id + "";
	}

	public boolean equals(Object obj) {
		if (obj instanceof IntegerToken) {
			IntegerToken t = (IntegerToken) obj;
			return (this.id == t.id);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return id;
	}

	public boolean isTerminal() {
		return terminal;
	}

	public String toString() {
		return getId();
	}

}
