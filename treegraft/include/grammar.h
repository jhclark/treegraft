#ifndef GRAMMAR_H_
#define GRAMMAR_H_

#include <stdio.h>
#include <string.h>

#include <tg_generics.h>
#include <key.h>
#include <string_utils.h>
#include <tokens.h>

typedef struct __grammarT {
	boolT useTopDownPredictions;
	grule_vecT* rule_list;
	tkn2grulevec_hashmapT* rules;
	tkn2posvec_hashmapT* words;
} grammarT;

extern grammarT* grammar_load(const char* filename);

#endif /*GRAMMAR_H_*/
