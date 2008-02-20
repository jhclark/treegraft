#ifndef KEY_H_
#define KEY_H_

#include <arc.h>

typedef struct __keyT {
	int id;
	int size;
	arcT* arc;
	tokenT lhs;
} keyT;

#endif /*KEY_H_*/
