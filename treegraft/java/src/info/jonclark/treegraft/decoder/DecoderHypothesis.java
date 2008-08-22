package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scored;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.Parse;

import java.util.List;

public class DecoderHypothesis<T extends Token> implements Scored {

	private final List<Parse<T>> parses;
	private final List<T> tokens;
	private FeatureScores featureScores;

	public DecoderHypothesis(List<Parse<T>> parses, List<T> tokens, FeatureScores featureScores) {
		this.parses = parses;
		this.tokens = tokens;
		this.featureScores = featureScores;
	}
	
	public List<Parse<T>> getParses() {
		return parses;
	}

	public List<T> getTokens() {
		return tokens;
	}

	public double getLogProb() {
		return featureScores.getLogProb();
	}

	public FeatureScores getScores() {
		return featureScores;
	}
}
