package info.jonclark.treegraft.decoder;

import info.jonclark.treegraft.chartparser.Chart;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.core.forestunpacking.parses.Parse;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.scoring.Scores;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.lm.LanguageModel;
import info.jonclark.treegraft.search.Beam;

import java.util.ArrayList;
import java.util.List;

public class BeamSearchDecoder<R extends GrammarRule<T>, T extends Token> {

	private final TokenFactory<T> tokenFactory;
	private final LanguageModel<T> lm;

	public BeamSearchDecoder(TokenFactory<T> tokenFactory, LanguageModel<T> lm) {
		this.tokenFactory = tokenFactory;
		this.lm = lm;
	}

	public List<Hypothesis<T>> getKBest(Chart<R, T> chart, ForestUnpacker<R, T> unpacker, int k) {

		// keep k-best translations for each non-terminal type and each span
		// by walking up the hypergraph, bottom-up

		final int N = chart.getInputLength();
		Beam<Hypothesis<T>>[][] beams = new Beam[N + 1][N + 1];
		for (int i = 0; i < beams.length; i++) {
			for (int j = 0; j < beams[i].length; j++) {
				beams[i][j] = new Beam<Hypothesis<T>>(k);
			}
		}

		// for each source key, keep a beam of the best hypotheses that it can
		// create each backpointer from higher-level keys will have access to
		// only the partial parses in that beam
		for (Key<R, T> key : chart.getKeys()) {
			List<Parse<T>> partialParses = unpacker.getPartialParses(key);
			for (Parse<T> parse : partialParses) {

				// create a hypothesis for each partial parse coming out of the
				// transfer stage
				Hypothesis<T> hyp = new Hypothesis<T>(parse);
				beams[key.getStartIndex()][key.getEndIndex()].add(hyp);
			}
		}

		// TODO: Unknown word handling

		// now that we've seeded our possible translations, start propagating
		// possibilities up through the spans

		// iterate over possible beams to output to
		for (int outputStart = 0; outputStart < N; outputStart++) {
			for (int outputEnd = outputStart + 1; outputEnd < N + 1; outputEnd++) {

				// now iterate over pairs of beams to read from
				for (int input1Start = 0; input1Start < N; input1Start++) {
					for (int input1End = input1Start + 1; input1End < N + 1; input1End++) {

						for (int input2Start = input1End; input2Start < N; input2Start++) {
							for (int input2End = input2Start + 1; input2End < N + 1; input2End++) {

								// create a new output hypothesis by joining the
								// 2 input hypotheses
								Beam<Hypothesis<T>> outputBeam = beams[outputStart][outputEnd];
								Beam<Hypothesis<T>> beam1 = beams[input1Start][input1End];
								Beam<Hypothesis<T>> beam2 = beams[input2Start][input2End];

								// TODO: Apply cube pruning right here
								for (Hypothesis<T> hyp1 : beam1) {
									for (Hypothesis<T> hyp2 : beam2) {

										Hypothesis<T> combinedHyp = concatenateHypotheses(hyp1, hyp2);
										outputBeam.add(combinedHyp);
									}
								}
							}
						}
					}
				}
			}
		}

		// return the hypotheses that cover the whole input
		return beams[0][beams.length];
	}

	private Hypothesis<T> concatenateHypotheses(Hypothesis<T> hyp1, Hypothesis<T> hyp2) {
		List<T> combinedTokens =
				new ArrayList<T>(hyp1.getTokens().size()
						+ hyp2.getTokens().size());
		combinedTokens.addAll(hyp1.getTokens());
		combinedTokens.addAll(hyp2.getTokens());

		// TODO: Fix this completely screwed up
		// method of combining LM scores
		// TODO: Add fragmentation penalty
		// TODO: Add length penalty
		// TODO: Add bidirectional lexical
		// scores
		TokenSequence<T> tokenSequence =
				tokenFactory.makeTokenSequence(combinedTokens);
		double lmScore = lm.score(tokenSequence);

		Scores combinedScore =
				new Scores(hyp1.getLogProb() + hyp2.getLogProb()
						+ lmScore);

		Hypothesis<T> combinedHyp =
				new Hypothesis<T>(combinedTokens, combinedScore);
		return combinedHyp;
	}
}
