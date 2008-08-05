#include <string_utils.h>

/* Return a pointer to the last character of s in set */
char* strrpbrk(char* s, const char* set) {
	assert(s != NULL);
	assert(set != NULL);
	
	const char* x;
	char* r;
	for (r = s + strlen(s) - 1; r >= s; r--)
		for (x = set; *x; x++)
			if (*r == *x)
				return r;
	return NULL;
}

boolT contains_char(const char c, const char* set) {
	assert(set != NULL);
	
	const char* x;
	for (x = set; *x; x++)
		if (c == *x)
			return TRUE;
	return FALSE;
}

/* Destructively trim a string */
char* trim(char* str) {
	assert(str != NULL);
	
	char* end= str + strlen(str) - 1;
	for (; end >= str && contains_char(*end, WHITESPACE); end--)
		;
	if (end != NULL)
		*(end+1) = '\0';
	for (; contains_char(*str, WHITESPACE); str++)
		;
	return str;
}

/*
 * Non-destructively get a substring
 * 
 * Caller is responsible for freeing result
 * */
char* substring(const char* str, int start, int end) {
	assert(start >= 0);
	assert(end >= 0);
	assert(start <= strlen(str));
	assert(end <= strlen(str));
	
	int n = end - start;
	char* dup = calloc(n, sizeof(char));
	strncpy(dup, str + start, n);
	dup[n] = '\0';
	return dup;
}

/*
 * Non-destructively get a substring before a delim
 * 
 * Caller is responsible for freeing result
 */
char* substring_before(const char* in, const char* delim, boolT return_delims) {
	assert(in != NULL);
	assert(delim != NULL);

	char* start = strstr(in, delim);
	if (start != NULL) {
		int span = (int) (start - in);
		if (return_delims) {
			return substring(in, 0, span + strlen(delim)); // cheap call to strlen()
		} else {
			return substring(in, 0, span);
		}
	} else {
		return strdup(in);
	}
}

/*
 * Non-destructively get a substring after a delim
 * 
 * Caller is responsible for freeing result
 */
char* substring_after(const char* in, const char* delim, boolT return_delims) {
	assert(in != NULL);
	assert(delim != NULL);

	char* start = strstr(in, delim);

	if (start != NULL && (start+1) != NULL) {
		int span = (int) (start - in);
		if (return_delims)
			return substring(in, span, strlen(in));
		else
			return substring(in, span + strlen(delim), strlen(in)); // cheap call to strlen()
	} else {
		return strdup(in);
	}
}

/* Destructively (via trim) counts tokens in a string */
int count_tokens(char* in, const char* delims) {
	assert(in != NULL);
	assert(delims != NULL);
	
	int n = 0;
	const char* ptr;
	for (ptr=trim(in); ptr; n++)
		ptr = strpbrk(ptr + 1, delims);
	return n;
}

/*
 * Destructively tokenizes a string into a vector
 */
str_vecT* tokenize_fast(char* in, const char* delims) {
	assert(in != NULL);
	assert(delims != NULL);
	
	int i;
	char* token;
	const char* delim = delims;
	int n = count_tokens(in, delims);
	str_vecT* vec = str_vec_new(n);

	in = trim(in);
	token = strtok(in, delims);
	for (i=0; token != NULL; i++) {
		str_vec_add(vec, token);
		token = strtok(NULL, delims);
	}

	return vec;

}

//int main() {
//	char* orig = strdup(" hello cruel world ");
//	char* str = orig;
//	printf("trim 0: %s\n", str);
//	str = trim(str);
//	printf("trim 1: %s\n", str);
//	str = trim(str);
//	printf("trim 2: %s\n", str);
//	free(orig);
//
//	orig = strdup(" hello cruel world ");
//	str = orig;
//	printf("count tokens: %d\n", count_tokens(str, WHITESPACE));
//	str_vecT* vec = tokenize_fast(str, WHITESPACE);
//	int i;
//	for (i=0; i<vec->size; i++)
//		printf("token %d: %s\n", i, vec->data[i]);
//	free(orig);
//
//	orig = strdup(" hello cruel world ");
//	str = orig;
//	char* before = substring_before(str, "cruel", FALSE);
//	char* after = substring_after(str, "cruel", FALSE);
//	printf("before: %s\n", before);
//	printf("after: %s\n", after);
//	free(before);
//	free(after);
//	free(orig);
//
//	return 0;
//}