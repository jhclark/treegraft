package info.jonclark.treegraft.decoder;

import info.jonclark.lang.NullOptions;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.parses.BasicTreeFormatter;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.parses.TreeFormatter;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

@OptionsTarget(NullOptions.class)
public class BasicLatticeFormatter<R extends GrammarRule<T>, T extends Token> implements
		LatticeFormatter<R, T> {

	private final TreeFormatter<T> treeFormatter;

	public BasicLatticeFormatter(NullOptions opts, TreegraftConfig<R, T> config) {
		this.treeFormatter = new BasicTreeFormatter<T>(config.tokenFactory, true, true);
	}

	public String format(Lattice<R, T> lattice) {

		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lattice.getInputLength(); i++) {
			for (int j = 0; j < lattice.getInputLength(); j++) {
				for (PartialParse<T> parse : lattice.getPartialParses(i, j)) {
					builder.append(i + " " + j + " " + parse.toString(treeFormatter) + "\n");
				}
			}
		}

		builder.append("\n");
		return builder.toString();
	}

}
