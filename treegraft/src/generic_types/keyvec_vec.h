#ifndef KEYVEC_VEC_H_
#define KEYVEC_VEC_H_

#include <tg_generics.h>
#include <key_vec.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(key_vecT*,keyvec,gen_grow_quadratic)
GEN_VECTOR_IMPL(key_vecT*,keyvec,gen_grow_quadratic)

#endif /*KEYVEC_VEC_H_*/
