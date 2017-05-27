#!/bin/bash

exists()
{
	command -v "$1" >/dev/null 2>&1
}

state() {
	eval $terminal "\"java -classpath $classpath client.TestApp $1 STATE; read\" &"
	sleep 0.1
}

launch_peer() {
	eval $terminal "\"java -Djavax.net.ssl.keyStore=keystore.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore.keys -Djavax.net.ssl.trustStorePassword=123456 -Dfile.encoding=UTF-8 -classpath $classpath server.Server $1 $2 $3 $4; bash\" &"
	sleep 0.1
}

os=$(uname)

if [ "$os" = "Linux" ]; then ##Figure out how to know terminal name
	if  exists urxvt ; then
		terminal=$(echo urxvt)
	elif  exists x-terminal-emulator ; then
		terminal=$(echo x-terminal-emulator)
	elif  exists gnome-terminal ; then
		terminal=$(echo gnome-terminal)
	else
		exit 1
	fi

	terminal=$(echo $terminal -e $BASH -c)
elif [ "$os" = "Darwin" ]; then
	terminal=$(echo open -a Terminal.app)
else
	exit 1
fi

compilePath=$(echo out/production/distributed-backup-system/)
#sh compile.sh $compilePath

classpath=$(realpath $compilePath)

#pushd .
#cd $classpath
#rmiregistry &
#popd

state "1" 
state "2" 
state "3" 
state "4" 
state "5" 
state "6" 

trap "exit" INT TERM
trap "kill 0" EXIT
wait
