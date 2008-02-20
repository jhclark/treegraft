#ifndef KEY_VEC_H_
#define KEY_VEC_H_

#include <tg_generics.h>
#include <key.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(keyT*,key,gen_grow_quadratic)
GEN_VECTOR_IMPL(keyT*,key,gen_grow_quadratic)

#endif /*KEY_VEC_H_*/
