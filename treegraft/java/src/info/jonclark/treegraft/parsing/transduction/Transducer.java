package info.jonclark.treegraft.parsing.transduction;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

public interface Transducer<R extends GrammarRule<T>, T extends Token> {
	
	/**
	 * Gets the desired ordering of the RHS constituents for transduction. The
	 * format of these alignments is defined in {@link
	 * SyncCFGRule.getTargetToSourceAlignment()}.
	 * 
	 * @param key
	 *            the key for which RHS alignments are desired
	 * @param rule
	 *            the rule for which RHS alignments are desired
	 * @return an array with alignment information as defined in
	 *         <code>SyncCFGRule.getAlignment()</code>
	 */
	public int[] getTargetToSourceRhsAlignment(R rule);
	

	/**
	 * Given a key and a specific <code>GrammarRule</code>, transduce its RHS to
	 * some array of terminals and non-terminals. In the case of a monolingual
	 * parse, this can simply be the original source RHS.
	 * 
	 * @param key
	 *            the key for which a transduced RHS is desired
	 * @param rule
	 *            the rule for which a transduced RHS is desired
	 * @return the transduced RHS
	 */
	public T[] transduceRhs(R rule);
	
	public T transduceLhs(R rule);
}
