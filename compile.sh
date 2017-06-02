#!/bin/bash
mkdir -p $1 &>/dev/null
javac -sourcepath src/ -d $1 src/client/TestApp.java src/server/Server.java
