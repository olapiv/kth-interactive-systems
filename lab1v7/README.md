# Introduction to Lab Chat

The system presented here consists of a chatserver and a client.
The server and the clients are both Jini applications, so they depend
on a Jini infrastructure being present in order to find each other.

Furthermore, both server and client are deployed in a distributed
fashion. This means that they are not run directly from local files.
Instead, a small and general bootstrap program (JarRunner.jar) is
used, and when executed with the correct parameters it fetches the
real program from a web server and runs it.

The assignment is to select a development task from a list of
alternatives (given separately), and then to modify the system
according to that task.

This distribution has two directories:
1. [develop](develop): The application sources and the files needed to compile and build
    them.
1. [test](test): Files that start the Jini service infrastructure and run the
    chatserver and client.
    

You must choose a webserver (a default is provided)


# Intended Learnings from Assignment

1. An application deployed on-demand from a webserver is much easier to maintain for a large group of users, since 
upgrades only are done on the webserver. No need to run around among users or rely on their ability to perform the 
upgrade themselves.
1. A separation of the development and test environment reflects how the program will actually be used. When the 
compiler and the runtime are on two different computer systems it is much easier to spot incompatibilities between them.
1. Dynamic discovery of services through the Jini middleware illustrates flexibility in a client-server or peer-to-peer
system. Services and clients can be added, moved, and removed dynamically, without having to reconfigure the components.

# Development platform

The distribution as given was verified (August 2018) on Ubuntu
version 18, and Windows 7. At this time the Java source level was
raised from Java 1.4 to be compatible with Java 5, 6, 7, and 8.

Required:
- Java SDK 1.5 or later
Helpful:
- ant - available from ant.apache.org and is used to compile, build, and install the software. Instructions for ant are 
in the file [build.xml](./develop/build.xml). As an alternative to ant, the [develop/bat](./develop/bat) directory contains Windows BAT-files that help with compilation, building, 
and installation.

In order to build and install the software by other means than ant or Windows BAT-files, the general outline is this 
(see [build.xml](./develop/build.xml) for details).

1. Compile the sources into class files, using the provided libraries. The [develop/build](./develop/build) directory 
is the recommended target for class files.
1. Put the class files in develop/build into jar-files, using the manifest files in [develop/mf](./develop/mf). Put the 
jar-files in the [develop/dist](./develop/dist) directory.
1. Copy the jar-files in [develop/dist](./develop/dist) to [test/cbs](./test/cbs).
1. (Once) Make sure that the files in [develop/lib](./develop/lib) ([jini-core.jar](./develop/lib/jini-core.jar), 
[jini-ext.jar](./develop/lib/jini-ext.jar), and  [reggie-dl.jar](./develop/lib/reggie-dl.jar)) are copied to the 
[test/cbs](./test/cbs) directory.


# Deployment and codebase webserver
A critical component of the system is the codebase server. This consists of a web-server that can serve class-files to 
the applications as they need them. A small web-server is provided with this distribution, and its default codebase 
(the directory from which it serves its files) is in the [test/cbs](test/cbs) directory. The script 
[test/bin/pc/r1_httpd.bat](test/bin/pc/r1_httpd.bat) starts the web-server and its code is in the 
[test/lib/tools.jar](test/lib/tools.jar) archive which is part of the Jini distribution.

However, if you have access to another webserver which you think would be better for you, you can use that. Just 
remember to change the launching scripts in test/bin/{pc,unix} to provide the correct URL to the applications as they 
start, and to arrange for an easy way to upload new versions of the jar files each time you rebuild the system.

The CHAT system needs the following files in the codebase server:
- [test/cbs/jini-core.jar](test/cbs/jini-core.jar)   The Jini middleware
- [test/cbs/jini-ext.jar](test/cbs/jini-ext.jar)    The Jini middleware
- [test/cbs/reggie-dl.jar](test/cbs/reggie-dl.jar)   The Jini middleware
- [test/cbs/ChatServer.jar](test/cbs/ChatServer.jar)  The chat server
- [test/cbs/ChatClient.jar](test/cbs/ChatClient.jar)  The chat client


# Testing platform

The chat-server and client will not find each other unless a Jini
lookup service (reggie) is running. The lookup server is actually an
RMI service, so it needs rmid (the RMI daemon) running on its
computer.

# Installing

1. Unzip the distribution. If you pick a non-default directory for installation, edit the "path to webserver directory"
in [develop/build.xml](develop/build.xml).
1. Verify that you are able to:
    - Compile the sources
    - Install jar-files on the webserver:
        * jini-core.jar              copied from develop/lib
        * jini-ext.jar               copied from develop/lib
        * reggie-dl.jar              copied from develop/lib
        * ChatClient.jar             compiled from sources
        * ChatServer.jar             compiled from sources
1. Once the webserver is running, verify that files can be fetched by opening a web browser and pointing it to the 
URLs of the files.

It is important that you are able to automate the installation on the webserver, because it is otherwise easy to forget 
that step when chasing a bug over frequent recompilations.

# Running the System

## Windows

### Jini middleware
1. Start **HTTP server** by running ```test/bin/pc/r1_httpd.bat```. It opens in a separate, minimized window labelled 
httpd. To shutdown the HTTP server, close that window.
1. Start **RMI deaemon** by running ```test/bin/pc/r2_rmid.bat```. It opens in a separate, minimized window. To 
shutdown the RMI daemon (and reggie), give the command rmid -stop in any command window with a listening prompt.
1. Start **Jini lookup server (reggie)** by running ```test/bin/pc/r3_reggie.bat```. Upon startup it registers itself with 
rmid as an activatable service. As a result, later output from the lookup server is printed in the rmid window.

### Chat server
Run ```test/bin/pc/chatserver.bat```. Use -h tag to see commandline options and exit.

### Chat client
Run ```test/bin/pc/chatclient.bat```.

## Linux, Mac

### Jini middleware
1. Start **HTTP server** by running ```test/bin/unix/r1_httpd.sh```. To shut it down, type ctrl+c twice.
1. Start **RMI daemon** by running ```test/bin/unix/r2_rmid.sh```. To shutdown the RMI daemon (and reggie), give the 
command ```rmid -stop``` in some other command shell (terminal).
1. Start **Jini lookup server (reggie)** by running ```test/bin/unix/r3_reggie.sh```. Upon startup it registers itself 
with rmid as an activatable service. As a result, later output from the lookup server is printed in the rmid window.

### Chat server
Run ```test/bin/unix/chatserver.sh```. Use -h tag to see commandline options and exit.

### Chat client
Run ```test/bin/unix/chatclient.sh```.

# Testing

The Jini middleware, i.e. the local webserver, rmid and the lookup
server (reggie) can be left running while you start and stop the
chat-server and clients (in their own command windows).

It is not always necessary to restart rmid and reggie after each
recompile, but it is of course the only way to be sure of a clean
restart. The lookup server (reggie) may be holding on to
registrations for at least 5 minutes after the service has died, so
there is a risk of old or stale entries conflicting with new code.
