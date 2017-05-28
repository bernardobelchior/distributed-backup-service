# Distributed Backup Service
A reliable and secure Distributed Backup Service for the Internet.

## Students
[Bernardo Belchior](https://github.com/bernardobelchior1) - up201405381 

[Edgar Passos](https://github.com/edgarlpassos) - up201404131

[João Gomes](https://github.com/joaogomes04) - up201403275

[Nuno Freitas](https://github.com/nunofreitas96) - up201404739

## Running
Our program uses a system called JSSE in order to ensure encrypted connections.
As such, the following arguments must be given to the Java Virtual Machine:
```
java -Djavax.net.ssl.keyStore=<filename1> -Djavax.net.ssl.keyStorePassword=<password1> -Djavax.net.ssl.trustStore=<filename2> - Djavax.net.ssl.trustStorePassword=<password2> <name-of-the-program> ... 
```

There are two keystores and a truststore provided in the repository for testing.
Their passwords are "123456", without quotes.

In order to run the server, a rmiregistry must be run, which is done as follows:
```
rmiregistry
```

### Server

To run the first server, use the following command (with the provided stores):
```
java -Djavax.net.ssl.keyStore=keystore.keys -Djavax.net.ssl.keyStorePassword=123456 -Djavax.net.ssl.trustStore=truststore.keys -Djavax.net.ssl.trustStorePassword=123456 server.Server <peer-access-point> <port-number> [<peer-ip-address> <peer-port-number>
```
The peer access point is the name to connect to with a TestApp. The port number identifies where the server will open its socket in. 
The peer IP address and port number must not be specified on the first peer of the network and must be specified on all others. It is used to start the process of joining the network.

### TestApp

To run the TestApp, use the following command:
```
java client.TestApp <peer-access-point> <operation>
```
The peer access point is the one given when starting the server we want to connect to; the operation is what identifies what to test and has the following possible values:
* `STATE`
* `BACKUP <filename>`
* `RESTORE <key> <path-to-output>`
* `DELETE <key>`

## Purpose of the Application 
Improve the Distributed Backup Service made in the first assignment, making it more robust and fault-tolerant, while allowing it to be used throughout the internet.

## Server Features 
* Backup 
* Restore
* Delete
* Security
  * Chunk encryption
* Fault-tolerant

## Target Platforms 
Java standalone application for PC/Mac

## Additional Improvements (if time permits)
* Peers authentication using certificates
* Space management
