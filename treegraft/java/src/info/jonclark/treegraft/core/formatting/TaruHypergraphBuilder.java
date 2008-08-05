package info.jonclark.treegraft.core.formatting;

import hyperGraph.HGVertex;
import hyperGraph.HyperGraph;
import info.jonclark.treegraft.chartparser.Key;
import info.jonclark.treegraft.core.rules.GrammarRule;
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
public class TaruHypergraphBuilder<R extends GrammarRule<T>, T extends Token> extends
		ParseForestFormatter<R, T, HyperGraph> {

	private HyperGraph graph;
	private TokenFactory<T> tokenFactory;
	private final HashMap<Key<R, T>, Integer> ids = new HashMap<Key<R, T>, Integer>();

	private static final String TERMINAL_TYPE = "X";
	private static final int DEFAULT_EDGE_INDEX = -1;
	private static final int[] DEFAULT_KINDEX = { -1, -1 };

	// pack target side insertions

	public TaruHypergraphBuilder(HyperGraph graph, TokenFactory<T> tokenFactory) {
		this.graph = graph;
		this.tokenFactory = tokenFactory;
	}

	@Override
	public void addNonterminal(Key<R, T> key) {
		String type = tokenFactory.getTokenAsString(key.getRule().getLhs());
		createVertexAndHyperedges(key, type, "");
	}

	@Override
	public void addTerminal(Key<R, T> key) {
		
		// TODO: What if a single terminal key has multiple translations?
		// TODO: This is only dealing with the source side; what about target
		// sides?
		String words = tokenFactory.getTokenAsString(key.getWord());
		createVertexAndHyperedges(key, TERMINAL_TYPE, words);
	}

	private void createVertexAndHyperedges(Key<R, T> key, String type, String words) {

		// create vertex
		HGVertex vertex = new HGVertex(key.getStartIndex(), key.getEndIndex(), type);
		int id = graph.addVertex(vertex);
		ids.put(key, id);

		Hypothesis hypothesis = new Hypothesis(words, DEFAULT_EDGE_INDEX, DEFAULT_KINDEX);
		vertex.addHypothesis(hypothesis);

		// TODO: Inserted RHS items? -1, -1 for inserted target items

		// create edge
		ArrayList<Key<R, T>>[] backpointers = key.getActiveArc().getBackpointers();

		String ruleId = key.getRule().getRuleId();
		int goal = id;
		int[] items = new int[backpointers.length];

		permuteEdgesIntoHyperedges(items, backpointers, ruleId, goal, 0);
	}

	private void permuteEdgesIntoHyperedges(int[] items, ArrayList<Key<R, T>>[] backpointers,
			String ruleId, int goal, int position) {

		for (Key<R, T> key : backpointers[position]) {
			int item = ids.get(key);
			items[position] = item;

			if (position < backpointers.length - 1) {
				permuteEdgesIntoHyperedges(items, backpointers, ruleId, goal, position + 1);
			} else {
				graph.addEdge(items, goal, ruleId);
			}
		}
	}

	@Override
	public int[] getRhsAlignment(Key<R, T> key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T[] transduce(Key<R, T> key) {
		// TODO Auto-generated method stub
		return null;
	}

}
