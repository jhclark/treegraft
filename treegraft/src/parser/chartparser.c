#include <chart.h>
#include <grammar.h>
#include <key.h>
#include <tkn2arcvec_hashmap.h>
#include <token_vec.h>
#include <grule_vec.h>

// TODO: Fix massive memory leaks
// TODO: vec_add_all
// TODO: vec_put
// TODO: gen_queue

keyT* KEY_S;

void parser_add_arc(tkn2arcvec_hashmapT** active_arcs,
		arc_vecT* completed_arcs, arcT* arc) {

	int j = arc->end;
	tokenT needs = arc_get_needed_constituent(arc);
	if (needs != NULL) {
		arc_vecT* list = tkn2arcvec_hashmap_get(active_arcs[j], needs);
		if (list == NULL) {
			list = arc_vec_new(10);
			tkn2arcvec_hashmap_put(active_arcs[j], needs, list);
		}
		arc_vec_add(list, arc);
	} else {
		arc_vec_add(completed_arcs, arc);
	}
}

/**
 * Advance the "dots" that iterates over the RHS's of arcs.
 */
void parser_extend_arcs(tkn2arcvec_hashmapT** active_arcs,
		arc_vecT* completed_arcs, keyT* key) {

	// NOTE: ambiguity packing occurs here

	int j = key->arc->start;
	tokenT needs = key->lhs;
	arc_vecT* affected_arcs = tkn2arcvec_hashmap_get(active_arcs[j], needs);
	arcT* arc;
	int k;

	if (affected_arcs != NULL) {
		for (k=0; k<affected_arcs->size; k++) {
			arc = arc_vec_get(affected_arcs, k);
			//			assert(key->lhs .equals(arc.getGrammarRule().getRhs()[arc.getDot()]) : "Key cannot expand this rule: LHS mismatch (key:"
			//			+ key.toString() + " arc:" + arc.toString() + ")";

			arcT* extended_arc = arc_extend(arc, key);
			parser_add_arc(active_arcs, completed_arcs, extended_arc);
		}
	}
}

chartT* parse(grammarT* grammar, token_vecT* input) {

	int i;
	int j;
	arc_queueT* agenda;
	arc_vecT* completed_arcs = arc_vec_new(10);
	chartT* chart = malloc(sizeof(chartT));

	// for comparison, in java: HashMap<String, ArrayList<ActiveArc>>[] activeArcs;
	// length of the hashmap array is the input size
	tkn2arcvec_hashmapT** active_arcs = calloc(input->size,
			sizeof(tkn2arcvec_hashmapT*));

	for (i=0; i<input->size; i++) {
		active_arcs[i] = malloc(sizeof(tkn2arcvec_hashmapT));
	}

	// step 1
	i=0;
	do {

		// step 2
		if (agenda->size == 0 && i < input->size) {
			token_vecT* all_pos = grammar_get_pos(input->data[i]);
			for (j=0; j<all_pos->size; j++) {
				keyT* pos_key = key_init(i, i+1, all_pos->data[j],
						input->data[i]);
				arc_queue_add(pos_key);
			}
			i++;
		}

		if (agenda->size == 0)
			break;

		// step 3
		keyT* key = key_queue_poll(agenda);
		key_start_timer(key);

		// step 4
		grule_vecT* rules = grammar_get_rules_starting_with(key);
		for (j=0; j<rules->size; j++) {
			arcT* extended_arc = arc_new(key->arc->start, key->arc->end,
					arc_length(key->arc), rules->data[j]);
			arc_add_backpointer(extended_arc, 0, key);

			parser_add_arc(active_arcs, completed_arcs, extended_arc);
		}

		// step 5
		parser_extend_arcs(active_arcs, completed_arcs, key);

		// step 6
		for (j=0; j<completed_arcs->size; j++) {
			arcT* completed_arc = arc_vec_get(completed_arcs, j);
			keyT* new_key = key_new(completed_arc);
			agenda_add(agenda, new_key);
		}
		completed_arcs->size = 0;

		// step 7
		chart_add(chart, key);

		// step 8 (handled in Chart);
		if (key->lhs == KEY_S && key->arc->start == 0 && key->arc->end
				== input->size) {
			chart_add_parse(chart, key);
		}

		key_stop_timer(key);
	} while (!queue_is_empty(agenda) || i < input->size);

	// we have now evaluated the CFG backbone
	// if we got a full parse tree, only evaluate constraints for that
	// otherwise, evaluate constraints for the whole chart

	// TODO: Free() keys and chart
	// TODO: Macros for template-type expansion of collection classes

	return chart;
}

int main(int argc, char** argv) {

	int i;
	
	// init globals
	KEY_S = key_new("S");

	grammarT* g = grammar_load("data/test.gra");

	char** str_tokens = tokenize("dogs bark");
	tokenT* tokens = pool(str_tokens);

	chartT* c = parse(g, tokens);
	parse_vecT* parses = get_grammatical_parses(c);
	printf("%d parses found.", parses->size);

	for (i=0; i<parses->size; i++) {
		printf("%s\n", parse_to_string(parses->data[i]));
	}
	
	// TODO: free()
	
	// free globals
	free(KEY_S);
}
