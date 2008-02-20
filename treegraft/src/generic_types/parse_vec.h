#ifndef PARSE_VEC_H_
#define PARSE_VEC_H_

#include <tg_generics.h>
#include <parse.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(parseT*,parse,gen_grow_quadratic)
GEN_VECTOR_IMPL(parseT*,parse,gen_grow_quadratic)

#endif /*PARSE_VEC_H_*/
