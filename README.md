
IMPORTANT NOTICE: PROJECT DEPRECATED
This project has become dormant for quite some time now. If you're looking for an efficient syntactic SMT decoder, I recommend cdec. I use it for my own research and commit code to it from time to time. Feel free to use this code for education, but there's far better decoders these days.

Summary
A transduction parser is the basis for NLP tasks such as paraphrasing and machine translation. This parser supports transduction via weighted synchronous context free grammar rules.

# treegraft
Automatically exported from code.google.com/p/treegraft


treegraft is currently in the beta stage of development by Jonathan Clark (http://www.cs.cmu.edu/~jhclark).

Getting Started
For examples of how to use treegraft as a parser, take a look at ChartParserTest (the treegraft JUnit tests).

As an MT decoder, treegraft is based on the Statistical Transfer concept from Carnegie Mellon's Avenue MT group.

To test treegraft from the terminal, you can run:

# make utest
To use treegraft as a decoder, modify data/treegraft.properties to suite your needs and run:

# treegraft.sh data/treegraft.properties
Information on loading plugins such as parsers, forest unpackers/decoders, lattice decoders and features at runtime via config files will be posted soon. Also, check back for a Moses-style "full pipeline" training script that will build a system given training data.

Documentation
Also, consider taking a look at the Javadoc API
