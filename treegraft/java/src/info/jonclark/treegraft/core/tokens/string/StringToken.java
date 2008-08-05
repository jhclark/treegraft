package info.jonclark.treegraft.core.tokens.string;

import info.jonclark.treegraft.core.tokens.Token;

public class StringToken implements Token {

	protected final String str;
	protected final boolean terminal;

	protected StringToken(String str, boolean terminal) {
		this.str = str.intern();
		this.terminal = terminal;
	}

	public String getId() {
		return str;
	}

	public boolean equals(Object obj) {
		if (obj instanceof StringToken) {
			StringToken tok = (StringToken) obj;
			
			// make use of quick comparison for interned strings
			return (this.str == tok.str);
		} else if(obj instanceof String) {
			String other = (String) obj;
			
			return other.equals(this.str);
		} else {
			return false;
		}
	}

	public int hashCode() {
		return str.hashCode();
	}

	public boolean isTerminal() {
		return terminal;
	}

	public String toString() {
		return str;
	}
}
