package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scored;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A grammatical parse for a given input sequence. (could be a single inserted
 * target word too). This class's factory is also responsible for appending the
 * <s> and </s> markers to the target-side token sequences.
 */
public class Parse<T extends Token> implements Scored {

	// TODO: Include the path through the source-side forest along with this
	// parse so that we can also recover the non-transduced version.

	private ArrayList<T> targetTokens = new ArrayList<T>();
	private Tree<T> sourceTree;
	private Tree<T> targetTree;
	private FeatureScores currentScore;

	private final List<T> sourceInputTokens;
	private final int startIndex;
	private final int endIndex;

	/**
	 * Create a new <code>Parse</code> from a non-terminal node
	 * 
	 * @param sourceInputTokens
	 *            The FULL source input sequence
	 * @param lispTree
	 */
	protected Parse(List<T> sourceInputTokens, int startIndex, int endIndex, T sourceLhs,
			T targetLhs, T[] sourceRhs, T[] targetRhs, FeatureScores scores) {

		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.sourceInputTokens = sourceInputTokens;
		this.sourceTree = new Tree<T>(sourceLhs, null, sourceRhs.length);
		this.targetTree = new Tree<T>(targetLhs, null, targetRhs.length);
		this.currentScore = scores;
	}

//	/**
//	 * Create a new <code>Parse</code> from a source terminal.
//	 * 
//	 * @param sourceInputTokens
//	 * @param startIndex
//	 * @param endIndex
//	 * @param sourceTerminal
//	 * @param scores
//	 */
//	protected Parse(List<T> sourceInputTokens, int startIndex, int endIndex, T sourceTerminal,
//			FeatureScores scores) {
//
//		this.sourceInputTokens = sourceInputTokens;
//		this.startIndex = startIndex;
//		this.endIndex = endIndex;
//		this.sourceTree = new Tree<T>(sourceTerminal, null, 0);
//		this.currentScore = scores;
//	}

	/**
	 * Create a new <code>Parse</code> from a target terminal.
	 * 
	 * @param sourceInputTokens
	 * @param targetTerminal
	 * @param targetTerminals
	 *            A list containing the targetTerminal and possibly a <s> or
	 *            </s> marker.
	 * @param scores
	 */
	protected Parse(List<T> sourceInputTokens, List<T> targetTerminals, T targetTerminal) {

		this.sourceInputTokens = sourceInputTokens;
		this.startIndex = -1;
		this.endIndex = -1;
		this.targetTree = new Tree<T>(targetTerminal, null, 0);
		this.currentScore = null;
		this.targetTokens.addAll(targetTerminals);
	}

	/**
	 * Copy constructor for parse
	 * 
	 * @param other
	 */
	public Parse(Parse<T> other) {
		this.currentScore = other.currentScore;
		this.startIndex = other.startIndex;
		this.endIndex = other.endIndex;
		this.sourceInputTokens = other.sourceInputTokens;
		this.targetTokens = new ArrayList<T>(other.targetTokens);
		this.sourceTree = new Tree<T>(other.sourceTree);
		this.targetTree = new Tree<T>(other.targetTree);
	}

	public void addRecombinedParse(Parse<T> parse) {
		// TODO: XXX: Track this!
	}

	public double getLogProb() {
		return currentScore.getLogProb();
	}

	public FeatureScores getScores() {
		return currentScore;
	}

	public void setCurrentScore(FeatureScores currentScore) {
		this.currentScore = currentScore;
		this.targetTree.setScores(currentScore);

		if (sourceTree != null)
			this.sourceTree.setScores(currentScore);
	}

	public void appendParse(int sourceRhsIndex, int targetRhsIndex, Parse<T> parse) {

		if (sourceRhsIndex != -1 && parse.sourceTree != null)
			sourceTree.addChild(sourceRhsIndex, parse.sourceTree);

		if (parse.targetTree != null)
			targetTree.addChild(targetRhsIndex, parse.targetTree);
		
		targetTokens.addAll(parse.targetTokens);
	}

	public void appendSourceTerminal(int sourceRhsIndex, T terminal) {
		// it is okay to use null here instead of a default
		// score since we aren't scoring the tree's, we're scoring the parses
		sourceTree.addChild(sourceRhsIndex, new Tree<T>(terminal, null, 0));
	}

	public List<T> getSourceTokens() {
		if (startIndex == -1 && endIndex == -1) {
			return Collections.EMPTY_LIST;
		} else {
			return sourceInputTokens.subList(startIndex, endIndex);
		}
	}

	public List<T> getTargetTokens() {
		return targetTokens;
	}

	public Tree<T> getSourceTree() {
		return sourceTree;
	}

	/**
	 * THIS COULD BE -1 FOR TARGET-ONLY "PARSES"
	 * 
	 * @return
	 */
	public int getStartIndex() {
		return startIndex;
	}

	/**
	 * THIS COULD BE -1 FOR TARGET-ONLY "PARSES"
	 * 
	 * @return
	 */
	public int getEndIndex() {
		return endIndex;
	}

	public Tree<T> getTargetTree() {
		return targetTree;
	}

	/**
	 * Get the string representation of this parse.
	 */
	public String toString() {
		return targetTree.toString();
	}
}
