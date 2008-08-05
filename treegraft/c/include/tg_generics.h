#ifndef TG_GENERICS_H_
#define TG_GENERICS_H_

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include <tg_types.h>

#include <gen_hash.h>
#include <gen_grow.h>
#include <gen_equals.h>

#include <gen_vector.h>
#include <gen_queue.h>
#include <gen_hashmap.h>

#include <arc.h>
#include <grule.h>
#include <key.h>
#include <key_node.h>
#include <parse.h>

// arc_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(arcT*,arc,gen_grow_quadratic)

// arc_queueT
// #define GEN_QUEUE_HEADER(T,NAME,GROWTH_FUNC) 
GEN_QUEUE_HEADER(arcT*,arc,gen_grow_quadratic)

// grule_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(gruleT*,grule,gen_grow_quadratic)

// key_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(keyT*,key,gen_grow_quadratic)

// keyvec_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(key_vecT*,keyvec,gen_grow_quadratic)

// keynode_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(key_nodeT*,keynode,gen_grow_quadratic)

// parse_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(parseT*,parse,gen_grow_quadratic)

// pos_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(posT,pos,gen_grow_quadratic)

// sent_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(sentT,sent,gen_grow_quadratic)

// str_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(char*,str,gen_grow_quadratic)

// str2tkn_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(char*,tokenT,str2tkn,0,gen_grow_quadratic,gen_hash_str,gen_equals_str)

// tkn2arcvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,arc_vecT*,tkn2arcvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2grulevec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,grule_vecT*,tkn2grulevec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2keyvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,key_vecT*,tkn2keyvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2posvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_HEADER(tokenT,pos_vecT*,tkn2posvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// token_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(tokenT,token,gen_grow_quadratic)


#endif /*TG_GENERICS_H_*/
