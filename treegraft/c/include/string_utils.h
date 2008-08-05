#ifndef STRING_UTILS_
#define STRING_UTILS_

#define WHITESPACE " \t\r\n"

#include <tg_types.h>
#include <tg_generics.h>
#include <assert.h>


/* Return a pointer to the last character of s in set */
extern char* strrpbrk(char* s, const char* set);

extern boolT contains_char(const char c, const char* set);

/* Destructively trim a string */
extern char* trim(char* str);

/*
 * Non-destructively get a substring
 * 
 * Caller is responsible for freeing result
 * */
extern char* substring(const char* str, int start, int end);

/*
 * Non-destructively get a substring before a delim
 * 
 * Caller is responsible for freeing result
 */
extern char* substring_before(const char* in, const char* delim, boolT return_delims);

/*
 * Non-destructively get a substring after a delim
 * 
 * Caller is responsible for freeing result
 */
extern char* substring_after(const char* in, const char* delim, boolT return_delims);

/* Destructively (via trim) counts tokens in a string */
extern int count_tokens(char* in, const char* delims);

/*
 * Destructively tokenizes a string into a vector
 */
extern str_vecT* tokenize_fast(char* in, const char* delims);

#endif /*STRING_UTILS_*/
