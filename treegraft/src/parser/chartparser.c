chart_t* parse(grammar_t grammar, token_vec_t* input) {

	queue_t agenda;
	chart_t chart = malloc(sizeof(chart_t));
	active_arc_box_t man;
	init_active_arc_box(man, input->size);

	// step 1
	int i = 0;

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
		key_t* key = queue_poll(agenda);
		key_start_timer(key);

		// step 4
		grammar_rule_vec_t* rules = grammar_get_rules_starting_with(key);
		for (int j=0; j<rules->size; j++) {
			active_arc_t* extended_arc = active_arc_init(key->start, key->end,
					key_length(key), rules->data[j]);
			active_arc_add_backpointer(extended_arc, 0, key);
			active_arc_box_add(man, extended_arc);
		}

		// step 5
		extend_arcs(man, key);

		// step 6
		active_arc_vec_t* completed_arcs = get_and_clear_completed_arcs(man);
		for (int j=0; j<completed_arcs->size; j++) {
			key_t* new_key = key_init(completed_arc);
			agenda_add(agenda, new_key);
		}

		// step 7
		chart_add(chart, key);

		// step 8 (handled in Chart);
		if (key->lhs == KEY_S && key.start == 0 && key.end == input->size) {
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
	grammar_t g = load_grammar("data/test.gra");
	
	string_t* str_tokens = tokenize("dogs bark");
	token_t* tokens = pool(str_tokens);
	
	chart_t c = parse(g, tokens);
	parse_vec_t* parses = get_grammatical_parses(c);
	printf("%d parses found.", parses.size);
	
	for (int i=0; i<parses.size; i++) {
		printf("%s\n", parse_to_string(parses.data[i]));
	}
	// TODO: free()
}
