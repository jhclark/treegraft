#ifndef KEYNODE_VEC_H_
#define KEYNODE_VEC_H_

#include <tg_generics.h>
#include <key_node.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(key_nodeT*,keynode,gen_grow_quadratic)
GEN_VECTOR_IMPL(key_nodeT*,keynode,gen_grow_quadratic)

#endif /*KEYNODE_VEC_H_*/
