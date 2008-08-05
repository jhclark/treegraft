#include <tokens.h>

extern token_cacheT* token_cache_new() {
	token_cacheT* cache = malloc(sizeof(token_cacheT));
	cache->map
			= str2tkn_hashmap_new(DEFAULT_VOCAB_SIZE, DEFAULT_VOCAB_LOAD_FACTOR);
	cache->nextId = 1;
	return cache;
}

tokenT str_to_token(token_cacheT* cache, char* str) {
	tokenT tok = str2tkn_hashmap_get(cache->map, str);
	if (tok == 0) {
		tok = cache->nextId;
		str2tkn_hashmap_put(cache->map, str, tok);
		cache->nextId++;
	}
	return tok;
}

token_vecT* str_vec_to_token_vec(token_cacheT* cache, str_vecT* str_vec) {
	tokenT token;
	token_vecT* tokens = token_vec_new(str_vec->size);
	char** str;
	for (str = str_vec->data; str <= str_vec_last(str_vec); str++) {
		token = str_to_token(cache, *str);
		token_vec_add(tokens, token);
	}
	return tokens;
}