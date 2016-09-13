# Distributed Backup Service

## Source code

Clone the project using git:

```
git clone https://github.com/ei12134/distributed-backup-service.git
```

## Compilation

### Software requirements

JDK 8 and JRE 8 or newer

### Build process

Create a binary folder inside the project folder:
```
mkdir -p bin
```

Compile the source files inside the project folder:

```
javac -d bin/ src/core/*.java src/dispatchers/*.java src/message/*.java src/rmi/*.java src/utils/*.java src/database/*.java src/protocols/*.java src/testapp/*.java
```

Copy peer resources and make sure all peers have the same shared key: 
```
cp res/crypto/* res/img/* bin/
```

## Java<sup>TM</sup> Remote Method Invocation (RMI)

The RMI setup is automatic, however if there are any problems:
* Method 1: Restart of the servers followed by the launching of the TestApp
* Method 2: Close all instances and manually start the Java RMI registry:

 * On the Solaris<sup>TM</sup> Operating System: `rmiregistry &`

 * Or, on Windows platforms: `start rmiregistry`

## Distributed Backup Service (Dbs)

```
java Dbs <peerID> <mcAddress> <mcPort> <mdbAddress> <mdbPort>
```

* peerID - unique peer identification in the network
* mcAddress - multicast control channel ip address
* mcPort - multicast control channel port
* mdbAddress - multicast data backup channel ip address
* mdbPort - multicast data backup channel port

## Test application CLI (TestApp)

```
java TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2>
```

* peer_ap - peer access point - remote RMI object's stub unique peer identification
* sub_protocol - the sub protocol being tested, and must be one of: BACKUP, RESTORE, DELETE or RECLAIM.
* opnd_1 - either the path name of the file to backup/restore/delete, for the respective 3 subprotocols, or the amount of space to reclaim. In the latter case, the peer should execute the RECLAIM protocol, upon deletion of any chunk.
* opnd_2 - integer that specifies the desired replication degree and applies only to the backup protocol (or its enhancement)

## Test application GUI (TestAppGUI)

```
java TestAppGUI
```
