#ifndef STR_VEC_H_
#define STR_VEC_H_

#include <tg_generics.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(char*,str,gen_grow_quadratic)
GEN_VECTOR_IMPL(char*,str,gen_grow_quadratic)

#endif /*STR_VEC_H_*/
