#ifndef GRULE_VEC_H_
#define GRULE_VEC_H_

#include <tg_generics.h>
#include <grule.h>

// #define GEN_VECTOR_HEADER(T,NAME,GROWTH_FUNC) 
GEN_VECTOR_HEADER(gruleT*,grule,gen_grow_quadratic)
GEN_VECTOR_IMPL(gruleT*,grule,gen_grow_quadratic)

#endif /*GRULE_VEC_H_*/
