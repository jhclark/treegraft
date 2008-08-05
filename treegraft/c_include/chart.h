#ifndef CHART_H_
#define CHART_H_

#include <assert.h>

#include <tg_generics.h>
#include <grule.h>
#include <parse.h>
#include <arc.h>
#include <key.h>

typedef struct __chartT {
	tkn2keyvec_hashmapT* chart;
	key_vecT* parses;
} chartT;

extern void chart_add(chartT* chart, keyT* key);

extern void chart_add_parse(chartT* chart, keyT* key);

extern void chart_unpack_backpointers1(arcT* arc, str_vecT* current_list);

extern parse_vecT* chart_get_parses2(chartT* chart, arcT* arc);

extern parse_vecT* chart_get_parses1(chartT* chart, keyT* key);

extern parse_vecT* chart_get_parses4(arcT* arc);

extern parse_vecT* chart_get_parses3(keyT* key);

extern void chart_unpack2(keyT* parent, str_vecT* current_list);

extern void chart_unpack_node(keyT* parent, key_nodeT* tree, boolT right_most,
		parse_vecT* parses);

extern void
		chart_unpack_backpointers2(keyvec_vecT* backpointers, key_vecT* keys,
				int nBackpointer, key_nodeT* tree, boolT right_most,
				parse_vecT* parses);

extern boolT is_grammatical(chartT* chart);

extern parse_vecT* get_grammatical_parses(chartT* chart);

#endif /*CHART_H_*/
