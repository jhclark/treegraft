for project in src ../uglygenerics/src; do
    for dir in `find $project -type d | fgrep -iv svn`; do
	includes="-I$dir $includes"
    done
done
echo $includes

for file in `find src | fgrep -iv svn | egrep '\.c$'`; do
    files="$file $files"
done
echo $files

gcc $includes $files -o bin/treegraft