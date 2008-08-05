#ifndef KEY_H_
#define KEY_H_

#include <tg_types.h>
#include <arc.h>

typedef struct __keyT {
	int id;
	int size;
	arcT* arc;
	tokenT lhs;
} keyT;

extern keyT* key_new(int id, sizeT size, arcT* arc, tokenT lhs);

#endif /*KEY_H_*/
