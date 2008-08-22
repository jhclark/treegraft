package info.jonclark.treegraft.core.tokens.reference;

import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.ArrayList;
import java.util.List;

public class ReferenceTokenSequence implements TokenSequence<ReferenceToken> {

	private int hash = -1;
	private final List<ReferenceToken> tokens;

	public ReferenceTokenSequence(List<ReferenceToken> tokens) {
		this.tokens = tokens;
	}

	public List<ReferenceToken> getTokens() {
		return tokens;
	}

	public int hashCode() {
		if (hash == -1) {
			for (int i = 0; i < tokens.size(); i++) {

				// calculate tokenHash^i
				int tokenHash = tokens.get(i).hashCode();
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
			if (this.tokens.size() == other.tokens.size()) {

				for (int i = 0; i < tokens.size(); i++) {
					if (this.tokens.get(i).equals(other.tokens.get(i)) == false) {
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

	/**
	 * {@inheritDoc}
	 */
	public int length() {
		return tokens.size();
	}

	public TokenSequence<ReferenceToken> keepNLeftMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new ReferenceTokenSequence(this.tokens.subList(0, n));
		}
	}

	public TokenSequence<ReferenceToken> keepNRightMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new ReferenceTokenSequence(this.tokens.subList(this.tokens.size() - n,
					this.tokens.size()));
		}
	}

	public TokenSequence<ReferenceToken> prepend(TokenSequence<ReferenceToken> seq) {

		ArrayList<ReferenceToken> newTokens =
				new ArrayList<ReferenceToken>(this.tokens.size() + seq.getTokens().size());
		newTokens.addAll(seq.getTokens());
		newTokens.addAll(this.tokens);

		return new ReferenceTokenSequence(newTokens);
	}
}
