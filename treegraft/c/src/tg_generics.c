#include <tg_generics.h>


// arc_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(arcT*,arc,gen_grow_quadratic)

// arc_queueT
// #define GEN_QUEUE_HEADER(T,NAME,GROWTH_FUNC) 
GEN_QUEUE_IMPL(arcT*,arc,gen_grow_quadratic)

// grule_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(gruleT*,grule,gen_grow_quadratic)

// key_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(keyT*,key,gen_grow_quadratic)

// keyvec_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(key_vecT*,keyvec,gen_grow_quadratic)

// keynode_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(key_nodeT*,keynode,gen_grow_quadratic)

// parse_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(parseT*,parse,gen_grow_quadratic)

// pos_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(posT,pos,gen_grow_quadratic)

// sent_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(sentT,sent,gen_grow_quadratic)

// str_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(char*,str,gen_grow_quadratic)

// str2tkn_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_IMPL(char*,tokenT,str2tkn,0,gen_grow_quadratic,gen_hash_str,gen_equals_str)

// tkn2arcvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_IMPL(tokenT,arc_vecT*,tkn2arcvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2grulevec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_IMPL(tokenT,grule_vecT*,tkn2grulevec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2keyvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_IMPL(tokenT,key_vecT*,tkn2keyvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// tkn2posvec_hashmapT
// #define GEN_HASHMAP_HEADER(T,V,NAME,GROWTH_FUNC,HASH_FUNC,EQUALS_FUNC)
GEN_HASHMAP_IMPL(tokenT,pos_vecT*,tkn2posvec,0,gen_grow_quadratic,gen_hash_int,gen_equals_int)

// token_vecT
// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_IMPL(tokenT,token,gen_grow_quadratic)
