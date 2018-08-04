#!/bin/bash
# Repast Simphony Model Starter
# By Michael J. North and Jonathan Ozik
# 11/12/2007
# Note the Repast Simphony Directories.

REPAST_SIMPHONY_ROOT=./installer/repast.simphony.runtime_2.3.1
REPAST_SIMPHONY_LIB=$REPAST_SIMPHONY_ROOT/lib

# Define the Core Repast Simphony Directories and JARs
CP=$CP:$REPAST_SIMPHONY_ROOT/bin
CP=$CP:$REPAST_SIMPHONY_LIB/saf.core.runtime.jar
CP=$CP:$REPAST_SIMPHONY_LIB/commons-logging-1.1.2.jar
CP=$CP:$REPAST_SIMPHONY_LIB/javassist-3.17.1-GA.jar
CP=$CP:$REPAST_SIMPHONY_LIB/jpf.jar
CP=$CP:$REPAST_SIMPHONY_LIB/jpf-boot.jar
CP=$CP:$REPAST_SIMPHONY_LIB/log4j-1.2.16.jar
CP=$CP:$REPAST_SIMPHONY_LIB/xpp3_min-1.1.4c.jar
CP=$CP:$REPAST_SIMPHONY_LIB/xstream-1.4.7.jar
CP=$CP:$REPAST_SIMPHONY_LIB/xmlpull-1.1.3.1.jar
CP=$CP:$REPAST_SIMPHONY_LIB/commons-cli-1.2.jar
CP=$CP:./installer/org.codehaus.groovy_2.3.7.xx-201411061335-e44-RELEASE/lib/groovy-all-2.3.7.jar
CP=$CP:/home/bhagya/repos/anonymous/sources/WeddingGrid/bin
CP=$CP:./installer/WDu.jar

# Change to the Default Repast Simphony Directory

# Start the Model
java -cp $CP bnw.abm.intg.sync.wdwrapper.RECMainClass  ../WeddingGrid/WeddingGrid.rs 142 4800 0
