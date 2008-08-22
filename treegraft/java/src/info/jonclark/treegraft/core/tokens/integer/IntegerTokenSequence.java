package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.ArrayList;
import java.util.List;

public class IntegerTokenSequence implements TokenSequence<IntegerToken> {

	private int hash = -1;
	private final List<IntegerToken> tokens;

	public IntegerTokenSequence(List<IntegerToken> tokens) {
		this.tokens = tokens;
	}

	public List<IntegerToken> getTokens() {
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

		if (obj instanceof IntegerTokenSequence) {
			IntegerTokenSequence other = (IntegerTokenSequence) obj;
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

	public TokenSequence<IntegerToken> keepNLeftMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new IntegerTokenSequence(this.tokens.subList(0, n));
		}
	}

	public TokenSequence<IntegerToken> keepNRightMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new IntegerTokenSequence(this.tokens.subList(this.tokens.size() - n,
					this.tokens.size()));
		}
	}

	public TokenSequence<IntegerToken> prepend(TokenSequence<IntegerToken> seq) {

		ArrayList<IntegerToken> newTokens =
				new ArrayList<IntegerToken>(this.tokens.size() + seq.getTokens().size());
		newTokens.addAll(seq.getTokens());
		newTokens.addAll(this.tokens);

		return new IntegerTokenSequence(newTokens);
	}
}
