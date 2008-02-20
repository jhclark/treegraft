#include <chart.h>

chartT* parse(grammar_t grammar, token_vec_t* input) {

	queueT agenda;
	chartT* chart = malloc(sizeof(chart_t));
	arc_boxT man;
	init_active_arc_box(man, input->size);

	// step 1
	int i = 0;
	int j;

	do {

		// step 2
		if (queue_is_empty(agenda) && i < input->size) {
			token_vec_t* all_pos = grammar_get_pos(input->data[i]);
			for (int j=0; j<all_pos.size; j++) {
				key_t* pos_key = key_init(i, i+1, all_pos->data[j],
						input->data[i]);
				queue_enqueue(pos_key);
			}
			i++;
		}

		if (queue_is_empty(agenda))
			break;

		// step 3
		keyT* key = queue_poll(agenda);
		key_start_timer(key);

		// step 4
		grule_vecT* rules = grammar_get_rules_starting_with(key);
		for (j=0; j<rules->size; j++) {
			arcT* extended_arc = active_arc_init(key->start, key->end,
					key_length(key), rules->data[j]);
			arc_add_backpointer(extended_arc, 0, key);
			arc_box_add(man, extended_arc);
		}

		// step 5
		extend_arcs(man, key);

		// step 6
		arc_vecT* completed_arcs = get_and_clear_completed_arcs(man);
		for (j=0; j<completed_arcs->size; j++) {
			keyT* new_key = key_init(completed_arc);
			agenda_add(agenda, new_key);
		}

		// step 7
		chart_add(chart, key);

		// step 8 (handled in Chart);
		if (key->lhs == KEY_S && key->arc->start == 0 && key->arc->end == input->size) {
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
	
	grammarT g = load_grammar("data/test.gra");
	
	char** str_tokens = tokenize("dogs bark");
	tokenT* tokens = pool(str_tokens);
	
	chartT* c = parse(g, tokens);
	parse_vecT* parses = get_grammatical_parses(c);
	printf("%d parses found.", parses.size);
	
	for (i=0; i<parses.size; i++) {
		printf("%s\n", parse_to_string(parses.data[i]));
	}
	// TODO: free()
}
