#ifndef KEY_NODE_H_
#define KEY_NODE_H_

#include <key_vec.h>

typedef struct _key_nodeT {
	keyT* key;
	key_vecT* children;
} key_nodeT;

#endif /*KEY_NODE_H_*/
