package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scored;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.util.StringUtils;

import java.util.Collections;
import java.util.List;

public class DecoderHypothesis<T extends Token> implements Scored {

	private final int sourceStartIndex;
	private final int sourceEndIndex;
	private final List<PartialParse<T>> parses;
	private final List<T> tokens;
	private FeatureScores featureScores;

	public DecoderHypothesis(int sourceStartIndex, int sourceEndIndex, List<PartialParse<T>> parses,
			List<T> tokens, FeatureScores featureScores) {

		this.sourceStartIndex = sourceStartIndex;
		this.sourceEndIndex = sourceEndIndex;
		this.parses = parses;
		this.tokens = tokens;
		this.featureScores = featureScores;
	}

	public List<PartialParse<T>> getParses() {
		return parses;
	}

	public List<T> getTokens() {
		return tokens;
	}
	
	public int getSourceStartIndex() {
		return sourceStartIndex;
	}
	
	public int getSourceEndIndex() {
		return sourceEndIndex;
	}

	public double getLogProb() {
		return featureScores.getLogProb();
	}

	public FeatureScores getScores() {
		return featureScores;
	}

	public void setCurrentScore(FeatureScores recombinedScores) {
		this.featureScores = recombinedScores;
	}
	
	public void addRecombinedHypothesis(DecoderHypothesis<T> hyp) {
		// TODO: Track these!
	}
	
	public void clearRecombinedHypotheses() {
		// TODO: Track these!
	}
	
	public List<DecoderHypothesis<T>> getRecombinedHypotheses() {
		// TODO: Track these!
		return Collections.EMPTY_LIST;
	}
	
	public String toString() {
		return StringUtils.untokenize(tokens) + " :: " + featureScores.toString();
	}
}
