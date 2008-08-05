/*
 *  Simple example of a CUnit unit test.
 *
 *  This program (crudely) demonstrates a very simple "black box" 
 *  test of the standard library functions fprintf() and fread().
 *  It uses suite initialization and cleanup functions to open
 *  and close a common temporary file used by the test functions.
 *  The test functions then write to and read from the temporary
 *  file in the course of testing the library functions.
 *
 *  The 2 test functions are added to a single CUnit suite, and
 *  then run using the CUnit Basic interface.  The output of the
 *  program (on CUnit version 2.0-2) is:
 *
 *            CUnit : A Unit testing framework for C.
 *           http://cunit.sourceforge.net/
 *
 *       Suite: Suite_1
 *         Test: test of fprintf() ... passed
 *         Test: test of fread() ... passed
 *
 *       --Run Summary: Type      Total     Ran  Passed  Failed
 *                      suites        1       1     n/a       0
 *                      tests         2       2       2       0
 *                      asserts       5       5       5       0
 */

#include <grammar.h>
#include <string_utils.h>
#include "CUnit/Basic.h"

int init_suite1(void) {
	return 0;
}
int clean_suite1(void) {
	return 0;
}

void test_string_utils(void) {
	char* orig = strdup(" hello cruel world ");
	char* str = orig;
	printf("trim 0: %s\n", str);
	CU_ASSERT(1);
	str = trim(str);
	printf("trim 1: %s\n", str);
	str = trim(str);
	printf("trim 2: %s\n", str);
	free(orig);

	orig = strdup(" hello cruel world ");
	str = orig;
	printf("count tokens: %d\n", count_tokens(str, WHITESPACE));
	str_vecT* vec = tokenize_fast(str, WHITESPACE);
	int i;
	for (i=0; i<vec->size; i++)
		printf("token %d: %s\n", i, vec->data[i]);
	free(orig);

	orig = strdup(" hello cruel world ");
	str = orig;
	char* before = substring_before(str, "cruel", FALSE);
	char* after = substring_after(str, "cruel", FALSE);
	printf("before: %s\n", before);
	printf("after: %s\n", after);
	free(before);
	free(after);
	free(orig);
}

void test_grammar_load(void) {
	const char* FILE = "data/test.gra";
	//	grammarT* gram = grammar_load(file);
}

/* The main() function for setting up and running the tests.
 * Returns a CUE_SUCCESS on successful running, another
 * CUnit error code on failure.
 */
int main() {
	CU_pSuite pSuite = NULL;

	/* initialize the CUnit test registry */
	if (CUE_SUCCESS != CU_initialize_registry())
		return CU_get_error();

	/* add a suite to the registry */
	pSuite = CU_add_suite("TreeGraftSuite", init_suite1, clean_suite1);
	if (NULL == pSuite) {
		CU_cleanup_registry();
		return CU_get_error();
	}

	/* add the tests to the suite */
	/* NOTE - ORDER IS IMPORTANT - MUST TEST fread() AFTER fprintf() */
	if (NULL == CU_add_test(pSuite, "test of string_utils", test_string_utils) ||
	    NULL == CU_add_test(pSuite, "test of grammar_load()", test_grammar_load)) {
		CU_cleanup_registry();
		return CU_get_error();
	}

	/* Run all tests using the console interface */
	CU_basic_set_mode(CU_BRM_VERBOSE);
	CU_basic_run_tests();
	CU_cleanup_registry();
	return CU_get_error();
}

