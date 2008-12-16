package info.jonclark.treegraft.metrics;

import info.jonclark.treegraft.core.tokens.Token;

import java.util.List;

public interface Metric<T extends Token> {
	
	public List<HypothesisErrorStatistics> getHypothesisErrorStatistics(List<T> hypotheses);
	
	public double getScore(List<HypothesisErrorStatistics> errorStatistics);
	
	public boolean isBiggerBetter();
}
