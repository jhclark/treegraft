package info.jonclark.treegraft.core;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.decoder.Lattice;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.util.StringUtils;

import java.util.List;

public class Result<R extends GrammarRule<T>, T extends Token> {

	public final Throwable t;
	public final int nSentence;
	public final String inputSentence;
	public final List<PartialParse<T>> nBestList;
	public final Lattice<R, T> lattice;

	public Result(int nSentence, Throwable t) {
		// sentence failed to complete
		this.t = t;
		this.nSentence = nSentence;
		this.inputSentence = null;
		this.nBestList = null;
		this.lattice = null;
	}

	/**
	 * @param nSentence
	 *            0-based inputSentence number
	 * @param inputSentence
	 * @param nBestList
	 */
	public Result(int nSentence, String inputSentence, List<PartialParse<T>> nBestList,
			Lattice<R, T> lattice) {
		this.nBestList = nBestList;
		this.inputSentence = inputSentence;
		this.nSentence = nSentence;
		this.lattice = lattice;
		this.t = null;
	}

	public Lattice<R, T> getLattice() {
		return lattice;
	}

	public String toString() {
		if (t == null) {
			return "TODO";
		} else {
			return "FAILED TO TRANSLATED SENTENCE " + nSentence + "\n"
					+ StringUtils.getStackTrace(t);
		}
	}
}
