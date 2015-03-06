statdoc
=======

Statdoc analyses your statistics project and provides a javadoc like set of
documentation pages with the key information it can discover. This is quite 
useful if you want to document your projects for others, or retrieve 
information about code written by somebody else or of an old project.

The system is setup to be extendible and flexible, but it currently mainly
focuses on projects using Stata. 

features
========

- provide a comprehensive documentation of all files, scripts and variables 
found in a given folder and the semantical links them.
- On the spot descriptive statistics for all identified variables.
- Highly flexible template system for basically all output based on
[Apache Velocity](http://velocity.apache.org/).

how to run
==========

To document you statistics project, just drop the jar file into the directory
and double click it. If you have Java 7 installed a Console window should appear 
that shows you the progress.

*Note:* that Statdoc runs a comprehensive examination script for all data files it
can discover. If you have a lot of data files (.csv, .raw, .dta) or very large
data sets (thousands of variables, millions of observations) this process might
take quite some time.

You can also invoke Statdoc from the command line or directly from within Stata
(experimental). 

```
net from https://dl.dropboxusercontent.com/u/12198759/ado/
net install statdoc
```

You will have to restart Stata at this point.

```
cd <the/root/directory/of/your/project> // VERY IMPORTANT
statdoc
```

how to code
===========

Statdoc is setup as a maven project in eclipse, if you have this setup you 
should be ready to go except that you need to include a stata-sfi.jar in your 
classpath or exclude the Stata integration class (statadoc.Stata, see above).
If you do not want to deal with maven, you should be fine just including 
the apache velocity jar file with dependencies.

