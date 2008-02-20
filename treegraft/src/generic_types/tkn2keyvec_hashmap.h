#ifndef TKN2KEYVEC_HASHMAP_H_
#define TKN2KEYVEC_HASHMAP_H_

#include <tg_generics.h>
#include <key_vec.h>

// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,key_vecT*,tkn2keyvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)
GEN_HASHMAP_IMPL(tokenT,key_vecT*,tkn2keyvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

#endif /*TKN2KEYVEC_HASHMAP_H_*/
