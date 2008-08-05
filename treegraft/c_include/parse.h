#ifndef PARSE_H_
#define PARSE_H_

typedef struct __parseT {
	char* lisp_tree;
} parseT;

extern parseT* parse_new(char* lisp_tree);

#endif /*PARSE_H_*/
