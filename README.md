# Music Theory
What happens if we model a musical phrase as a mathematical object, perform algebraic operations like multiplication and inversion on it, then convert it back? The Java code in this project implements some "musical algebras" I've dreamt up. See a full report in report.pdf.

## Installation
Download the musictheory jar file and put it in your java classpath.
You will also need the JLinAlg library from jlinalg.sourceforge.net.

(If you run code through the commandline, add .jars to your java classpath via the CLASSPATH environment variable or the -cp option in the java and javac commands. If you're using one of them fancy eye-dee-ees, the process is different. `*shrug*`)

## Documentation
Code is (roughly) documented in Javadoc comments. You may ask Java to generate a Javadoc document at your leisure... Here I will briefly describe each package:

- musicthory.music - model music in a form compatible with our algebras
- musicthory.algebra - implement the algebras of interest
- musicthory.player - interface our musical models with the Java SoundAPI
