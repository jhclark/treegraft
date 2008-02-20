#ifndef TOKEN_VEC_H_
#define TOKEN_VEC_H_

#include <tg_generics.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(tokenT,token,gen_grow_quadratic)
GEN_VECTOR_IMPL(tokenT,token,gen_grow_quadratic)

#endif /*TOKEN_VEC_H_*/
