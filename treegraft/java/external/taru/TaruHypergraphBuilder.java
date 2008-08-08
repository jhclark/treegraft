package taru;

import hyperGraph.HGVertex;
import hyperGraph.HyperGraph;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.formatting.forest.ParseForestFormatter;
import info.jonclark.treegraft.core.synccfg.SyncCFGRule;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.util.ArrayList;
import java.util.HashMap;

import taruDecoder.Hypothesis;

/**
 * Calls only get made post-packing (unique source sides are ensured)
 * 
 * @author jon
 * @param <R>
 * @param <T>
 */
public class TaruHypergraphBuilder<R extends SyncCFGRule<T>, T extends Token> extends
		ParseForestFormatter<R, T, HyperGraph> {

	private HyperGraph graph;
	private TokenFactory<T> tokenFactory;
	private final HashMap<Key<R, T>, Integer> nonterminalIds = new HashMap<Key<R, T>, Integer>();
	private final HashMap<T, Integer> terminalIds = new HashMap<T, Integer>();

	public static final int DEFAULT_VERTEX_COUNT = 100000;

	private static final int INSERTION_START = -1;
	private static final int INSERTION_END = -1;
	private static final String TERMINAL_TYPE = "X";
	private static final int DEFAULT_EDGE_INDEX = -1;
	private static final int[] DEFAULT_KINDEX = { -1, -1 };

	// pack target side insertions

	public TaruHypergraphBuilder(TokenFactory<T> tokenFactory) {
		this.graph = new HyperGraph(DEFAULT_VERTEX_COUNT);
		this.tokenFactory = tokenFactory;
	}

	@Override
	public void addNonterminal(Key<R, T> key) {

		System.out.println("ADDING NONTERMINAL: " + key);

		String nonterminalType = tokenFactory.getTokenAsString(key.getRule().getLhs());
		HGVertex nonterminalVertex =
				new HGVertex(key.getStartIndex(), key.getEndIndex(), nonterminalType);
		int nonterminalId = graph.addVertex(nonterminalVertex);
		nonterminalIds.put(key, nonterminalId);

		// create edge
		ArrayList<Key<R, T>>[] backpointers = key.getActiveArc().getBackpointers();

		// check for any terminals coming off of this key
		for (T token : key.getRule().getTargetRhs()) {
			if (token.isTerminal()) {

				String words = tokenFactory.getTokenAsString(token);
				System.out.println("ADDING TERMINAL: " + words);

				HGVertex terminalVertex =
						new HGVertex(INSERTION_START, INSERTION_END, TERMINAL_TYPE);
				int terminalId = graph.addVertex(terminalVertex);
				terminalIds.put(token, terminalId);

				Hypothesis hypothesis = new Hypothesis(words, DEFAULT_EDGE_INDEX, DEFAULT_KINDEX);
				terminalVertex.addHypothesis(hypothesis);
			}
		}

		// prepare to turn edges into hyperedges
		String ruleId = key.getRule().getRuleId();
		T[] targetRhs = key.getRule().getTargetRhs();
		int[] items = new int[targetRhs.length];

		int[] alignment = key.getRule().getTargetToSourceAlignment();
		assert alignment.length == targetRhs.length : "length mismatch";

		permuteEdgesIntoHyperedges(key, items, alignment, targetRhs, backpointers, ruleId,
				nonterminalId, 0);
	}

	private void permuteEdgesIntoHyperedges(Key<R, T> parent, int[] items, int[] alignment,
			T[] targetRhs, ArrayList<Key<R, T>>[] backpointers, String ruleId, int goal,
			int targetPosition) {

		if (alignment[targetPosition] == -1) {

			// handle insertion of terminals, which will have only dummy
			// backpointers
			int terminalId = terminalIds.get(targetRhs[targetPosition]);
			items[targetPosition] = terminalId;

		} else {

			int sourcePosition = alignment[targetPosition];
			
			for (Key<R, T> sourceKey : backpointers[sourcePosition]) {

				Integer nonterminalId = nonterminalIds.get(sourceKey);
				if (nonterminalId == null) {
					throw new RuntimeException(
							"Child backpointer has not yet been assigned an ID: " + sourceKey.toString()
									+ " -- for parent: " + parent.toString());
				} else {
					items[targetPosition] = nonterminalId;

					if (targetPosition < targetRhs.length - 1) {

						// continue to the end of the target RHS rule...
						permuteEdgesIntoHyperedges(parent, items, alignment, targetRhs,
								backpointers, ruleId, goal, targetPosition + 1);
					} else {

						// ...and then add the edge
						graph.addEdge(items, goal, ruleId);
					}
				}
			}
		}
	}

	@Override
	public HyperGraph getParseForest() {
		return graph;
	}

}
