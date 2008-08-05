#include <parse.h>

parseT* parse_new(char* lisp_tree) {
	parseT* parse = malloc(sizeof(parseT));
	parse->lisp_tree = lisp_tree;
	return parse;
}