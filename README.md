# Distributed Backup Service
A reliable and secure Distributed Backup Service for the Internet.

## Students
[Bernardo Belchior](https://github.com/bernardobelchior1) - up201405381 

[Edgar Passos](https://github.com/edgarlpassos) - up201404131

[João Gomes](https://github.com/joaogomes04) - up201403275

[Nuno Freitas](https://github.com/nunofreitas96) - up201404739

## Running
Our program uses a system called JSSE in order to ensure encrypted connections.
As such, the following arguments must be given to the Java Virtual Machine
```
java -Djavax.net.ssl.keyStore=<filename1> -Djavax.net.ssl.keyStorePassword=<password1> -Djavax.net.ssl.trustStore=<filename2> - Djavax.net.ssl.trustStorePassword=<password2> <name-of-the-program> ... 
```

There are two keystores and a truststore provided in the repository for testing.
Their passwords are "123456", without quotes.

## Purpose of the Application 
Improve the Distributed Backup Service made in the first assignment, making it more robust and fault-tolerant, while allowing it to be used throughout the internet.

## server.Server Features 
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
