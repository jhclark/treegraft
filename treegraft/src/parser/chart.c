#include <assert.h>

#include <tkn2keyvec_hashmap.h>
#include <parse_vec.h>
#include <arc.h>
#include <key.h>

#define DEFAULT_CHART_SIZE = 1000;
typedef struct __chartT {
	tkn2keyvec_hashmapT* chart;
	key_vecT* parses;
} chartT;

void chart_add(chartT* chart, keyT* key) {
	// do ambiguity packing by virtue of id choice and hashing
	tkn2keyvec_hashmap_put(chart->chart, key->id, key);
}

void chart_add_parse(chartT* chart, keyT* key) {
	assert(chart_contains_key(chart, key->id));
	key_vec_add(chart->parses, key);
}

static inline parse_vecT* chart_get_parses1(chartT* chart, keyT* key) {
	return chart_get_parses2(chart, key->active_arc);
}

parse_vecT* chart_get_parses2(chartT* chart, arcT* arc) {
	// unpack ambiguities from this parse
	parse_vecT* parses = parse_vec_new(10);
	key_nodeT* root = key_node_new();

	ArrayList<Key>[] backpointers = arc.getBackpointers();
	Key[] keys = new Key[backpointers.length];
	unpackBackpointers(backpointers, keys, 0, root, true, parses);

	for (
			final Parse parse : parses) {
				parse.setRoot(arc);
			}
			return parses.toArray(new
			Parse[parses.size()]);
		}

parse_vecT* get_parses2(keyT* key) {
	return get_parses3(key->active_arc);
}

parse_vecT* get_parses3(arcT* arc) {

	ArrayList<StringBuilder> currentList = new ArrayList<StringBuilder>();
	currentList.add(new StringBuilder());

	unpackBackpointers(arc, currentList);

	parse_vecT* result = new Parse[currentList.size()];
	for (int i = 0; i < currentList.size(); i++) {
		String str = currentList.get(i).toString();
		result[i] = new Parse(str);
	}
	return result;
}

void unpack_backpointers(arcT* arc, ArrayList<StringBuilder> currentList) {

	// open parentheses
	for (int i = 0; i < currentList.size(); i++) {
		currentList.get(i).append("(" + arc.getGrammarRule().getLhs() + " ");
	}

	ArrayList<Key>[] backpointers = arc.getBackpointers();
	for (int i = 0; i < backpointers.length; i++) {
		assert(backpointers[i] != null);

		// expand the list to the size of the pack
		int nAmbiguities = backpointers[i].size();
		ArrayList<StringBuilder>[] miniList = new ArrayList[nAmbiguities];

		for (int j = 0; j < nAmbiguities; j++) {
			miniList[j] = new ArrayList<StringBuilder>(currentList.size());
			for(final StringBuilder sb : currentList)
			miniList[j].add(new StringBuilder(sb));
			unpack2(backpointers[i].get(j), miniList[j]);
		}

		currentList.clear();
		for (int j = 0; j < nAmbiguities; j++) {
			currentList.addAll(miniList[j]);
		}
	}

	// close off parentheses
	for (int i = 0; i < currentList.size(); i++) {
		currentList.get(i).append(")");
	}
}

void unpack2(keyT* parent, ArrayList<StringBuilder> currentList) {

	if(parent.isTerminal()) {
		for (int i = 0; i < currentList.size(); i++) {
			currentList.get(i).append(parent.toCNodeString());
		}
	} else {
		unpackBackpointers(parent.getActiveArc(), currentList);
	}
}

void unpack_node(keyT* parent, Node<Key> tree, boolean rightMost, ArrayList<Parse> parses) {
	if (parent.isTerminal()) {
		parses.add(tree.toParse());
	} else {
		ArrayList<Key>[] backpointers = parent.getActiveArc().getBackpointers();
		Key[] keys = new Key[backpointers.length];
		unpackBackpointers(backpointers, keys, 0, tree, rightMost, parses);
	}
}

void unpack_backpointers(ArrayList<Key>[] backpointers, keyT[] keys, int nBackpointer,
		Node<Key> tree, boolean rightMost, ArrayList<Parse> parses) {

	if (nBackpointer == backpointers.length) {
		for (int i = 0; i < keys.length; i++) {
			unpackNode(keys[i], tree.children.get(i), rightMost && i == keys.length - 1, parses);
		}
	} else {
		if (backpointers[nBackpointer] != null) {
			for (int i = 0; i < backpointers[nBackpointer].size(); i++) {
				keys[nBackpointer] = backpointers[nBackpointer].get(i);
				tree.children.add(new Node<Key>(keys[nBackpointer]));
				unpackBackpointers(backpointers, keys, nBackpointer + 1, tree, rightMost,
						parses);
				tree.children.remove(tree.children.size() - 1);
			}
		}
	}
}

boolT is_grammatical(chartT* chart) {
	return chart->parses->size != 0;
}

parse_vecT* get_grammatical_parses(chartT* chart) {

	int i;
	int j;
	keyT* k;
	parseT* p;
	parse_vecT* parses2;
	parse_vecT* grammatical_parses = parse_vec_new(10);

	for (i=0; i<chart->parses->size; i++) {
		k = parse_vec_get(chart->parses, i);
		parses2 = get_parses2(chart, k);
		for (j=0; j<parses2->size; j++) {
			p = parse_vec_get(parses2, j);
			parse_vec_add(grammatical_parses, p);
		}
	}
	return grammatical_parses;
}
