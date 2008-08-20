package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.forestunpacking.parses.Parse;
import info.jonclark.treegraft.core.scoring.Scored;
import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.List;

public class Hypothesis<T extends Token> implements Scored {

	private final List<T> tokens;
	private Scores score;
	
	public Hypothesis(Parse<T> parse) {
		this.tokens = parse.getTargetTokens();
		this.score = parse.getScores();
	}
	
	public Hypothesis(List<T> tokens, Scores score) {
		this.tokens = tokens;
		this.score = score;
	}

	public Hypothesis(List<T> tokens, double score) {
		this.tokens = tokens;
	}

	public List<T> getTokens() {
		return tokens;
	}

	public double getLogProb() {
		return score.getLogProb();
	}
}
