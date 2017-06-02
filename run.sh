#!/bin/bash

exists()
{
	command -v "$1" >/dev/null 2>&1
}

launch_peer() {
	eval $terminal "\"java -Djavax.net.ssl.keyStore=keystore.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore.keys -Djavax.net.ssl.trustStorePassword=123456 -Dfile.encoding=UTF-8 -classpath $classpath server.Server $1 $2 $3 $4; bash\" &"
	sleep 0.5
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

	terminal=$(echo $terminal -e bash -c)
elif [ "$os" = "Darwin" ]; then
	terminal=$(echo open -a Terminal.app)
else
	exit 1
fi

compilePath=$(echo out/production/distributed-backup-system/)
sh compile.sh $compilePath

classpath=$(realpath $compilePath)

pushd .
cd $classpath
rmiregistry &
popd

launch_peer "1" "1234"
launch_peer "2" "1235" "193.136.33.112" "1234"
launch_peer "3" "1236" "193.136.33.112" "1234"
launch_peer "4" "1237" "193.136.33.112" "1235"
launch_peer "5" "1238" "193.136.33.112" "1235"
launch_peer "6" "1239" "193.136.33.112" "1235"
launch_peer "7" "1240" "193.136.33.112" "1234"
launch_peer "8" "1241" "193.136.33.112" "1234"

trap "exit" INT TERM
trap "kill 0" EXIT
wait
