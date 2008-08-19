package info.jonclark.treegraft.core.forestunpacking.parses;

import info.jonclark.treegraft.core.scoring.Scored;
import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A grammatical parse for a given input sequence.
 */
public class Parse<T extends Token> implements Scored {

	// TODO: Include the path through the source-side forest along with this
	// parse so that we can also recover the non-transduced version.

	private ArrayList<T> targetTokens = new ArrayList<T>();
	private Tree<T> sourceTree;
	private Tree<T> targetTree;
	private Scores currentScore;

	/**
	 * Create a new <code>Parse</code> from its string representation.
	 * 
	 * @param lispTree
	 */
	public Parse(T sourceLhs, T targetLhs, T[] sourceRhs, T[] targetRhs) {
		this.sourceTree = new Tree<T>(sourceLhs, null, sourceRhs.length);
		this.targetTree = new Tree<T>(targetLhs, null, targetRhs.length);
		this.currentScore = new Scores(0.0);
	}

	public Parse(T terminal, boolean sourceSide) {
		if (sourceSide) {
			sourceTree = new Tree<T>(terminal, null, 0);
		} else {
			targetTree = new Tree<T>(terminal, null, 0);
			targetTokens.add(terminal);
		}
		this.currentScore = new Scores(0.0);
	}

	/**
	 * Copy constructor for parse
	 * 
	 * @param other
	 */
	public Parse(Parse<T> other) {
		this.currentScore = new Scores(other.currentScore);
		this.targetTokens = new ArrayList<T>(other.targetTokens);
		this.sourceTree = new Tree<T>(other.sourceTree);
		this.targetTree = new Tree<T>(other.targetTree);
	}

	public double getLogProb() {
		return currentScore.getLogProb();
	}

	public void setCurrentScore(Scores currentScore) {
		this.currentScore = currentScore;
		this.targetTree.setScores(currentScore);

		if (sourceTree != null)
			this.sourceTree.setScores(currentScore);
	}

	public void appendParse(int sourceRhsIndex, int targetRhsIndex, Parse<T> parse) {
		if (sourceRhsIndex != -1)
			sourceTree.addChild(sourceRhsIndex, parse.sourceTree);

		targetTree.addChild(targetRhsIndex, parse.targetTree);

		targetTokens.addAll(parse.targetTokens);
	}
	
	public void appendSourceTerminal(int sourceRhsIndex, T terminal) {
		sourceTree.addChild(sourceRhsIndex, new Tree<T>(terminal, null, 0));
	}

	public List<T> getTargetTokens() {
		return targetTokens;
	}

	public Tree<T> getSourceTree() {
		return sourceTree;
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
