HEADERS = -Iinclude -I../uglygenerics/src
SRCDIR = src
BINDIR	= bin
OBJDIR	= obj

MAKE	= make
MFLAGS	=
CC	= gcc
CFLAGS	= -g
LD	= gcc
LDFLAGS =
AR	= ar
ARFLAGS =
RANLIB	= ranlib
RM	= rm
ECHO	= echo

SHELL	= /bin/sh

SRCS=string_utils.c
OBJS=${SRCS:.c=.o}

# clear out all suffixes
.SUFFIXES:
# list only those we use
.SUFFIXES: .o .c

# define a suffix rule for .c -> .o
.c.o :
	$(CC) $(CFLAGS) $(HEADERS) -c $(SRCDIR)/$< -o $(OBJDIR)/$@

all: treegraft

treegraft: $(OBJS)
	$(LD) $(LDFLAGS) -o $(BINDIR)/$@ $(OBJS)

test: $(OBJS)
	$(CC) $(CFLAGS) $(HEADERS) -lcunit $(OBJS) test/treegraft_test.c -o $(BINDIR)/test
	bin/test

clean:
	-$(RM) -f $(OBJS)
	-for dir in $(DIRS); do (cd $$dir; $(MAKE) clean); done

clobber: clean
	-$(RM) -rf $(BINDIR)

help:
	@echo ""
	@echo "make		- Builds project"
	@echo "make treegraft	- Ditto"
	@echo "make clean	- Removes object files"
	@echo "make clobber	- Removes all generated files"
	@echo "make help	- This information screen"
	@echo ""

# do this if given an invalid target
#.DEFAULT :
#	@$(MAKE) help

