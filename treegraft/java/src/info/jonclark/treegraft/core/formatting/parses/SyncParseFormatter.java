package info.jonclark.treegraft.core.formatting.parses;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.util.FormatUtils;

/**
 * A ParseFormatter for a SyncCFGRule that allows outputting parses as a source
 * tree, target tree, or target string; This class is where the magic of
 * transduction from source to target is actually defined.
 */
public class SyncParseFormatter<T extends Token> extends ParseFormatter<SyncCFGRule<T>, T> {

	private final OutputType outputType;
	private final TokenFactory<T> tokenFactory;
	private final boolean showScores;

	public SyncParseFormatter(TokenFactory<T> tokenFactory, OutputType type, boolean showScores) {
		this.outputType = type;
		this.tokenFactory = tokenFactory;
		this.showScores = showScores;
	}

	public String formatNonterminalAfter(Key<SyncCFGRule<T>, T> key) {
		if (outputType == OutputType.SOURCE_TREE || outputType == OutputType.TARGET_TREE) {
			return ") ";
		} else {
			return "";
		}
	}

	public String formatNonterminalBefore(Key<SyncCFGRule<T>, T> key) {

		// open parentheses
		if (outputType == OutputType.SOURCE_TREE) {
			return "(" + tokenFactory.getTokenAsString(key.getRule().getLhs()) + " ";

		} else if (outputType == OutputType.TARGET_TREE) {
			if (showScores) {
				return "(" + tokenFactory.getTokenAsString(key.getRule().getTargetLhs()) + "="
						+ FormatUtils.formatDoubleExp(key.getLogProb()) + " ";
			} else {
				return "(" + tokenFactory.getTokenAsString(key.getRule().getTargetLhs()) + " ";
			}
		} else {
			return "";
		}
	}

	public String formatTerminal(T token) {

		if (outputType == OutputType.SOURCE_TREE) {
			return tokenFactory.getTokenAsString(token) + " ";
		} else if (outputType == OutputType.TARGET_TREE || outputType == OutputType.TARGET_STRING) {
			return tokenFactory.getTokenAsString(token) + " ";
		} else {
			throw new RuntimeException("Unknown type: " + outputType);
		}
	}

	public int[] getRhsAlignment(Key<SyncCFGRule<T>, T> key) {

		if (outputType == OutputType.SOURCE_TREE) {
			return super.getMonotonicAlignment(key.getRule().getRhs().length);
		} else if (outputType == OutputType.TARGET_TREE || outputType == OutputType.TARGET_STRING) {
			return key.getRule().getAlignment();
		} else {
			throw new RuntimeException("Unknown type: " + outputType);
		}

	}

	@Override
	public T[] transduce(Key<SyncCFGRule<T>, T> key) {

		if (outputType == OutputType.SOURCE_TREE) {
			return key.getRule().getRhs();
		} else if (outputType == OutputType.TARGET_TREE || outputType == OutputType.TARGET_STRING) {
			return key.getRule().getTargetRhs();
		} else {
			throw new RuntimeException("Unknown type: " + outputType);
		}

	}
}
