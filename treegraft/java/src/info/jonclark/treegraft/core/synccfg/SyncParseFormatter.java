package info.jonclark.treegraft.core.synccfg;

import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.formatting.parses.ParseFormatter;
import info.jonclark.treegraft.core.scoring.ParseScorer;
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
	private final ParseScorer<SyncCFGRule<T>, T> scorer;
	private final boolean showScores;

	public SyncParseFormatter(TokenFactory<T> tokenFactory, OutputType type,
			ParseScorer<SyncCFGRule<T>, T> scorer, boolean showScores) {
		
		this.outputType = type;
		this.tokenFactory = tokenFactory;
		this.scorer = scorer;
		this.showScores = showScores;
	}

	public String formatNonterminalAfter(Key<SyncCFGRule<T>, T> key, SyncCFGRule<T> rule, double score) {
		if (outputType == OutputType.SOURCE_TREE || outputType == OutputType.TARGET_TREE) {
			return ") ";
		} else {
			return "";
		}
	}

	public String formatNonterminalBefore(Key<SyncCFGRule<T>, T> key, SyncCFGRule<T> rule, double score) {

		// open parentheses
		if (outputType == OutputType.SOURCE_TREE) {
			return "(" + tokenFactory.getTokenAsString(key.getLhs()) + " ";

		} else if (outputType == OutputType.TARGET_TREE) {
			if (showScores) {
				return "(" + tokenFactory.getTokenAsString(rule.getTargetLhs()) + "="
						+ FormatUtils.formatDoubleExp(score) + " ";
			} else {
				return "(" + tokenFactory.getTokenAsString(rule.getTargetLhs()) + " ";
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

	public int[] getTargetToSourceRhsAlignment(Key<SyncCFGRule<T>, T> key, SyncCFGRule<T> rule) {

		if (outputType == OutputType.SOURCE_TREE) {
			return super.getMonotonicAlignment(rule.getRhs().length);
		} else if (outputType == OutputType.TARGET_TREE || outputType == OutputType.TARGET_STRING) {
			return rule.getTargetToSourceAlignment();
		} else {
			throw new RuntimeException("Unknown type: " + outputType);
		}

	}

	@Override
	public T[] transduce(Key<SyncCFGRule<T>, T> key, SyncCFGRule<T> rule) {

		if (outputType == OutputType.SOURCE_TREE) {
			return rule.getRhs();
		} else if (outputType == OutputType.TARGET_TREE || outputType == OutputType.TARGET_STRING) {
			return rule.getTargetRhs();
		} else {
			throw new RuntimeException("Unknown type: " + outputType);
		}

	}

	@Override
	public ParseScorer<SyncCFGRule<T>, T> getScorer() {
		return scorer;
	}
}
