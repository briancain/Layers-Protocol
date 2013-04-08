# Data Link Layer Protocol

This java program implements the Data Link Layer Protocol

## Run Arguments

If you're in eclipse, you can run the following java files in order by right clicking on them and running it as an application

        RunServer.java
        RunB.java
        RunA.java

If you want to compile from the command line, do the following:

Make sure temp.txt and topology.txt are in the same dir that you are running the project out of

        javac Node.java
        javac PhyServer.java

Then in three seperate terminals, in order, enter these commands

        java PhyServer topology.txt 1337
        java Node B localhost 1337
        java Node A localhost 1337

## Warning

If you are a student taking CIS 725, please remember you are still bound by the K-State Honor Code. Copying this code and passing it off as your own is a violation of that Honor Code.

## Todo

Relay Layer
