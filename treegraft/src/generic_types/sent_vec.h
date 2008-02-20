#ifndef SENT_VEC_H_
#define SENT_VEC_H_

#include <tg_generics.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(sentT,sent,gen_grow_quadratic)
GEN_VECTOR_IMPL(sentT,sent,gen_grow_quadratic)

#endif /*SENT_VEC_H_*/
