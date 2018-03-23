# Music Theory
What happens if we model a musical phrase as a mathematical object, perform algebraic operations like multiplication and inversion on it, then convert it back? The Java code in this project implements some "musical algebras" I've dreamt up. See a full report in report.pdf.

## Installation
I'm old-fashioned; I compile my projects from the command line. The only installation is downloading the src folder and running javac on it. The only tricky part is ensuring all dependencies are in the class path (the CLASSPATH environmnt variable or the -cp option). If you're using one of those fancy eye-dee-ees, the process is different.

Some day I will likely compile this into a .jar file, but it is not presently high-enough quality to justify that yet.

## Dependencies
One of the algebras (the Linear Algebra, implemented in src/algebra/Linear.java) makes extensive use of the JLinAlg library. Download it from jlinalg.sourceforge.net and make sure the .jar is included in your classpath.

## Documentation
Code is (roughly) documented in Javadoc comments. You may ask Java to generate a Javadoc document at your leisure... Here I will briefly describe each package:

music - model music in a form compatible with our algebras
algebra - implement the algebras of interest
player - interface our musical models with the Java SoundAPI
