# Makefile for CS322 Lab4.
#
JFLAGS = -g
JC = javac
JCC = javacc

.SUFFIXES: .java .class

.java.class:
	$(JC) $(JFLAGS) $*.java

scgen: 	ast/Ast0.class ast/Ast0Parser.class SC0Gen.class

clean:
	'rm' ast/*.class *.class


