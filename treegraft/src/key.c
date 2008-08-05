#include <key.h>

keyT* key_new(int id, sizeT size, arcT* arc, tokenT lhs) {
	keyT* key = malloc(sizeof(keyT));
	key->id = id;
	key->size = size;
	key->arc = arc;
	key->lhs = lhs;
	return key;
}