#ifndef KEY_NODE_H_
#define KEY_NODE_H_

#include <tg_generics.h>

typedef struct _key_nodeT {
	keyT* key;
	struct __keynode_vecT* children;
} key_nodeT;

#endif /*KEY_NODE_H_*/
