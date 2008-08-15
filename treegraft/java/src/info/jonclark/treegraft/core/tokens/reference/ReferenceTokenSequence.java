package info.jonclark.treegraft.core.tokens.reference;

import info.jonclark.treegraft.core.tokens.TokenSequence;

public class ReferenceTokenSequence implements TokenSequence<ReferenceToken> {

	private int hash = -1;
	private final ReferenceToken[] tokens;

	public ReferenceTokenSequence(ReferenceToken[] tokens) {
		this.tokens = tokens;
	}

	public ReferenceToken[] getTokens() {
		return tokens;
	}

	public int hashCode() {
		if (hash == -1) {
			for (int i = 0; i < tokens.length; i++) {

				// calculate tokenHash^i
				int tokenHash = tokens[i].hashCode();
				int pow = tokenHash;
				for (int j = 1; j < i; j++) {
					pow *= tokenHash;
				}
				hash += pow;
			}
		}
		return hash;
	}

	public boolean equals(Object obj) {
		
		if (obj instanceof ReferenceTokenSequence) {
			ReferenceTokenSequence other = (ReferenceTokenSequence) obj;
			if (this.tokens.length == other.tokens.length) {
				
				for (int i = 0; i < tokens.length; i++) {
					if (this.tokens[i].equals(other.tokens[i]) == false) {
						return false;
					}
				}
				return true;
				
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
