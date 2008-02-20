#ifndef ARC_VEC_H_
#define ARC_VEC_H_

#include <tg_generics.h>
#include <arc.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(arcT*,arc,gen_grow_quadratic)
GEN_VECTOR_IMPL(arcT*,arc,gen_grow_quadratic)

#endif /*ARC_VEC_H_*/
