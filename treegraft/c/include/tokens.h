#ifndef TOKENS_H_
#define TOKENS_H_

/* Responsible for converting strings to (integer) tokens */

#define DEFAULT_VOCAB_SIZE 10000
#define DEFAULT_VOCAB_LOAD_FACTOR 0.9

#include <tg_generics.h>

typedef struct _token_cacheT {
	str2tkn_hashmapT* map;
	tokenT nextId;
} token_cacheT;

extern token_cacheT* token_cache_new();

extern tokenT str_to_token(token_cacheT* cache, char* str);

extern token_vecT* str_vec_to_token_vec(token_cacheT* cache, str_vecT* str_vec);

#endif /*TOKENS_H_*/
