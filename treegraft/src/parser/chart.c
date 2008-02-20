#include <chart.h>

// TODO: vec_add_all
// TODO: vec_put

#define DEFAULT_CHART_SIZE 1000
#define MAX_CHARS_PER_PARSE 100000

void chart_add(chartT* chart, keyT* key) {
	// do ambiguity packing by virtue of id choice and hashing
	tkn2keyvec_hashmap_put(chart->chart, key->id, key);
}

void chart_add_parse(chartT* chart, keyT* key) {
	assert(chart_contains_key(chart, key->id));
	key_vec_add(chart->parses, key);
}

void chart_unpack_backpointers1(arcT* arc, str_vecT* current_list) {

	int i;
	int j;
	int k;
	char* str;
	keyT* parent;

	// open parentheses
	for (i = 0; i < current_list->size; i++) {
		str = str_vec_get(current_list, i);
		gruleT* rule = arc_get_grammar_rule(arc);

		strncat(str, "(", MAX_CHARS_PER_PARSE);
		strncat(str, grule_get_lhs(rule), MAX_CHARS_PER_PARSE);
		strncat(str, " ", MAX_CHARS_PER_PARSE);
	}

	keyvec_vecT* backpointers = arc_get_backpointers(arc);
	for (i = 0; i < backpointers->size; i++) {
		assert(backpointers->data[i] != 0);

		// expand the list to the size of the pack
		int num_ambiguities = key_get_length(backpointers[i]);
		str_vecT** mini_list = calloc(num_ambiguities, sizeof(str_vecT*));

		for (j = 0; j < num_ambiguities; j++) {
			mini_list[j] = str_vec_new(current_list->size);

			for (k=0; k<mini_list[j]->size; k++) {
				// create a sepearate buffer (parses branch from here)
				str = strdup(current_list->data[k]);
				str_vec_add(mini_list[j], str);
				parent = key_vec_get(key_vec_get(backpointers, i), j);
				chart_unpack2(parent, mini_list[j]);
			}

			current_list->size = 0;
			for (j = 0; j < num_ambiguities; j++) {
				str_vec_add_all(current_list, mini_list[j]);
			}
		}

		// close off parentheses
		for (i = 0; i < current_list->size; i++) {
			str = str_vec_get(current_list, i);
			strncat(str, ")", MAX_CHARS_PER_PARSE);
		}
	}
}

parse_vecT* chart_get_parses2(chartT* chart, arcT* arc) {

	int i;
	parseT* parse;

	// unpack ambiguities from this parse
	parse_vecT* parses = parse_vec_new(10);
	key_nodeT* root = key_node_new();

	key_vecT** backpointers = arc_get_backpointers(arc);
	keyT** keys = calloc(chart->parses->size, sizeof(keyT*));
	chart_unpack_backpointers2(backpointers, keys, 0, root, TRUE, parses);

	for (i=0; i<parses->size; i++) {
		parse = parse_vec_get(parses, i);
		parse_set_root(arc);
	}
	return parses;
}

parse_vecT* chart_get_parses1(chartT* chart, keyT* key) {
	return chart_get_parses2(chart, key->arc);
}

parse_vecT* chart_get_parses4(arcT* arc) {

	str_vecT* current_list = str_vec_new(10);
	int i;
	char* str;
	parseT* parse;

	// initialize with an empty string FROM THE HEAP
	str_vec_add(current_list, strdup(""));

	chart_unpack_backpointers1(arc, current_list);

	parse_vecT* result = parse_vec_new(current_list->size);
	for (i = 0; i < current_list->size; i++) {
		str = str_vec_get(current_list, i);
		parse = parse_new(str);
		parse_vec_put(i, parse);
	}
	return result;
}

parse_vecT* chart_get_parses3(keyT* key) {
	return chart_get_parses4(key->arc);
}

void chart_unpack2(keyT* parent, str_vecT* current_list) {

	int i;
	char* old_str;
	char* new_str;

	if (key_is_terminal(parent)) {
		for (i = 0; i < current_list->size; i++) {
			old_str = str_vec_get(current_list, i);
			key_get_c_node_str(parent);
		}
	} else {
		chart_unpack_backpointers1(parent->arc, current_list);
	}
}

void chart_unpack_node(keyT* parent, key_nodeT* tree, boolT right_most,
		parse_vecT* parses) {

	parseT* parse;

	if (key_is_terminal(parent)) {
		parse = key_node_to_parse(tree);
		parse_vec_add(parses, parse);
	} else {
		arcT* arc = key_get_arc(parent);
		keyvec_vecT* backpointers = arc_get_backpointers(arc);
		key_vecT* keys = key_vec_new(backpointers->size);
		chart_unpack_backpointers2(backpointers, keys, 0, tree, right_most,
				parses);
	}
}

void chart_unpack_backpointers2(keyvec_vecT* backpointers, key_vecT* keys,
		int nBackpointer, key_nodeT* tree, boolT right_most, parse_vecT* parses) {

	int i;
	key_vecT* backpointer;
	key_nodeT* subtree;
	key_nodeT* node;
	keyT* key;

	if (nBackpointer == backpointers->size) {
		for (i = 0; i < keys->size; i++) {
			key = key_vec_get(keys, i);
			subtree = keynode_vec_get(tree->children, i);
			chart_unpack_node(key, subtree, right_most && i == keys->size - 1,
					parses);
		}
	} else {
		backpointer = keyvec_vec_get(backpointers, nBackpointer);
		if (backpointer != 0) {
			for (i = 0; i < backpointer->size; i++) {
				key = key_vec_get(backpointer, i);
				key_vec_put(keys, i, key);

				node = malloc(sizeof(key_nodeT));
				node->key = key;
				key_vec_add(tree->children, node);
				chart_unpack_backpointers2(backpointers, keys, nBackpointer + 1,
						tree, right_most, parses);
				keynode_vec_remove(tree->children, tree->children->size - 1);
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
