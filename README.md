# SecurePhotoShareApplication
Secure Photo Share Application using Java

# How to run PhotoShare?

* 1º - Go to Folder src/Project/
* 2º - Compile PhotoShareServer and PhotoShareClient (and optionally AddUser)

  PhotoShareServer: javac -d . src/Project/PhotoShareServer.java src/Project/ServerOperations.java src/Project/MAC.java src/Project/Cifra.java 

  PhotoShareClient: javac -d . src/Project/PhotoShareClient.java src/Project/ClientOperations.java

  AddUser: javac -d . src/Project/AddUser.java src/Project/MAC.java

 
* 3º - Execute PhotoShareServer and PhotoShareClient on same folder

  PhotoShareServer
	
	  java -Djava.security.manager -Djava.security.policy==servidor.pol projeto1.PhotoShareServer 23456 <passwordMAC>

   PhotoShareClient

	  java -Djava.security.manager -Djava.security.policy==cliente.pol projeto1.PhotoShareClient -u <localUserId> -a <serverAddress> [ -p <photos> | -l <userId> | -g <userId> | -c <comment> <userId> <photo> | -f <followUserIds> | -n ] 

* Execution Example:

  PhotoShareServer

  java -Djava.security.manager -Djava.security.policy==servidor.pol project.PhotoShareServer 23456 123456

  PhotoShareClient

  java -Djava.security.manager -Djava.security.policy==cliente.pol project.PhotoShareClient -u manel:1234 -a 10.101.148.17:23456 -p mini.jpg
