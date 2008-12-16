all: treegraft utest

treegraft:
	mkdir -p bin
	javac -cp lib/JonClarkDotInfo.jar:lib/phramer.jar:lib/junit-4.4.jar -d bin `find src -name '*.java'`

utest:
	java -cp lib/JonClarkDotInfo.jar:lib/junit-4.4.jar:bin org.junit.runner.JUnitCore info.jonclark.treegraft.FullTest

clean:
	rm -rf bin