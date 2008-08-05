/*
 * This should be the only file that deals directly with mutilating strings,
 * so Unicode compatibility can be added by modifying only this file (in theory).
 */

#include <grammar.h>

// TODO: Add includes here


// TODO: 1) Create and test needed string functions
// TODO: 2) Finish and test grammar loading code


/**
 * Eventually, read any of these formats:
 *  
 * S -> NP VP
 * S -> NP (V NP)
 * 
 * S::S : [NP VP] -> [VP NP]
 * S::S : [NP (V NP)] -> [NP NP V]
 */


#define DEFAULT_RULE_COUNT 100
#define MAX_LINE_LENGTH 10000

/*
 * Load a grammar from a file.
 * 
 * The caller is responsible for freeing the grammar and its subelements.
 */
grammarT* grammar_load(const char* filename) {

	FILE* in = fopen(filename, "r");
	grammarT* grammar = malloc(sizeof(grammarT));
	grammar->rule_list = grule_vec_new(DEFAULT_RULE_COUNT);

	// TODO: Allocate rule_list in grammar

	char line[MAX_LINE_LENGTH];
	while (fgets(line, MAX_LINE_LENGTH, in)) {

		// parse full rules
		if (strstr(line, "=>")) {
			char* lhs = trim(substring_before(line, "=>", FALSE));
			char* after = trim(substring_after(line, "=>", FALSE));
			str_vecT* rhs_str = tokenize_fast(after, WHITESPACE);
			token_vecT* rhs_tokens = str_vec_to_token_vec(rhs_str);

			// TODO: Convert string vector into token vector

			gruleT* rule = grule_new(lhs, rhs_tokens);
			grule_vec_add(grammar->rule_list, rule);

			if (tkn2grulevec_hashmap_contains_key(grammar->rules, rhs_tokens[0])) {
				grule_vecT* rule_vec = tkn2grulevec_hashmap_get(grammar->rules,
						rhs_tokens->data[0]);
				grule_vec_add(rule_vec, rule);
			} else {
				// assume 1 rule per LHS and grow as needed
				grule_vecT* rule_vec = grule_vec_new(1);
				grule_vec_add(rule_vec, rule);
				tkn2grulevec_hashmap_put(grammar->rules, rhs_tokens->data[0], rule_vec);
			}

			// parse lexical rules
		} else if (strstr(line, "->")) {
			char* pos = trim(substring_before(line, "->", FALSE));
			char* word = trim(substring_after(line, "->", FALSE));

			// TODO: convert pos and word from strings to tokens

			if (tkn2posvec_hashmap_contains_key(grammar->words, word)) {
				pos_vecT* pos_vec =
						tkn2posvec_hashmap_get(grammar->words, word);
				pos_vec_add(pos_vec, word);
			} else {
				// assume 1 pos per word and grow as needed
				pos_vecT* pos_vec = pos_vec_new(1);
				pos_vec_add(pos_vec, word);
				tkn2posvec_hashmap_put(grammar->words, word, pos_vec);
			}
		}
	}
	fclose(in);
}

/**
 * Gets all valid parts of speech for a word. Returns NULL if no words were found.
 */
pos_vecT* grammar_get_pos(grammarT* grammar, tokenT word) {
	pos_vecT* pos = tkn2posvec_hashmap_get(grammar->words, word);
	return pos;
}

/**
 * Gets rules starting with a specified key (token). Returns NULL if no words were found.
 */
grule_vecT* get_rules_starting_with(grammarT* grammar, keyT* key) {
	if (grammar->useTopDownPredictions) {
		// TODO: Add top down predictions
		// (though this probably isn't that useful in practice since we can't get partial parses)
	}
	grule_vecT* result = tkn2grulevec_hashmap_get(grammar->rules, key->lhs);
	return result;
}
