#ifndef PARSE_H_
#define PARSE_H_

#include <key_node.h>
#include <arc.h>

typedef struct __parseT {
	key_nodeT tree;
	arcT* root;
	char* lispTree;
} parseT;

#endif /*PARSE_H_*/
