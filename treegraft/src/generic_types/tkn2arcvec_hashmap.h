#ifndef TKN2ARCVEC_HASHMAP_H_
#define TKN2ARCVEC_HASHMAP_H_

#include <tg_generics.h>
#include <arc_vec.h>

// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,arc_vecT*,tkn2arcvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)
GEN_HASHMAP_IMPL(tokenT,arc_vecT*,tkn2arcvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

#endif /*TKN2ARCVEC_HASHMAP_H_*/
