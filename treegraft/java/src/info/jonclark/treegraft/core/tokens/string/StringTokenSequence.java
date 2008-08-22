package info.jonclark.treegraft.core.tokens.string;

import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.ArrayList;
import java.util.List;

public class StringTokenSequence implements TokenSequence<StringToken> {

	private int hash = -1;
	private final List<StringToken> tokens;

	public StringTokenSequence(List<StringToken> tokens) {
		this.tokens = tokens;
	}

	public List<StringToken> getTokens() {
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

		if (obj instanceof StringTokenSequence) {
			StringTokenSequence other = (StringTokenSequence) obj;
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

	public TokenSequence<StringToken> keepNLeftMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new StringTokenSequence(this.tokens.subList(0, n));
		}
	}

	/**
	 * If there aren't that many tokens, it is automatically takes as many as
	 * possbile
	 */
	public TokenSequence<StringToken> keepNRightMostTokens(int n) {
		if (this.tokens.size() <= n) {
			return this;
		} else {
			return new StringTokenSequence(this.tokens.subList(this.tokens.size() - n,
					this.tokens.size()));
		}
	}

	public TokenSequence<StringToken> prepend(TokenSequence<StringToken> seq) {

		ArrayList<StringToken> newTokens =
				new ArrayList<StringToken>(this.tokens.size() + seq.getTokens().size());
		newTokens.addAll(seq.getTokens());
		newTokens.addAll(this.tokens);

		return new StringTokenSequence(newTokens);
	}
}
