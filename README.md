#Distributed File System

##Description
This was an academic project from a Distributed Systems course, developed by me and Alexandre Garcia.

It implements a secure community of servers where each server exposes an hierarchy of folders/files.
A server can operate with a local base folder or with a remote file system from a Dropbox or GoogleDrive account.
A client is able to join an existing community and operate the whole File System, in the sense that, each server folders/files hierarchy is abstracted in one single FileSystem. (Although, it supports operations in only one server).

Synchronization between a client's local system and a remote directory is supported.

The following commands are supported in the File System: 
- servers - list all the URL's from all the servers that belong to the community.
- ls dir - list all the files/folders in the folder dir ("." and ".." have the usual meaning), files with the same name are shown as "name@server".
- mkdir dir@server - creates a folder dir in the server "server".
- rmdir dir@server - removes the folder dir in the server "server".
- cp path1 path2 - copy file path1 to path2; A file in a specific server must be represented with path@server, a file in the client local file system must be represented as path@local.
- rm path - removes the file path.
- getattr path - list information about the file/folder path, including: name, boolean that denotes it it's a file and last modified date.

NOTE: The file/folder path can be given to a command with "@server", where server is a server address. By default, it's assumed that the operation will be performed in the whole File System.

##Technical Details
 
- The servers communicate with each other through RMI and WebServices.
- Each server communicates in only one of this technologies, although, they can all communicate with each other. To support this feature we implemented a "Connection Manager" that abstracts the protocol used by a server that has sent a message.
- Communications between RMI sockets are protected with SSL.
- The servers that access Dropbox and Google Drive services implement a proxy that uses the REST API with OAuth2 offered by this services to communicate.
- All the servers have a distributed list (with all the servers addresses in the community) that is always up to date. 
When a new server s_new joins the community it needs an address of one server s that already belongs to the community. 
The server that receives the s_new server hello sends the complete list of addresses of all servers.
- The distributed file system supports a mechanism that allows the client to find servers in the same network by broadcasting to the network. 
The first server that receives a broadcast request will respond with his address and all the server addresses that he knows.


##Instructions

###Server:


java DirServerWS [-p Port] [-bp BasePath] community [URL] serverIPAddress
java DirServerRMI [-bp BasePath] community [URL] serverIPAddress

The default port is 8080.
The default basePath is ".", the current dir.

 - Start 1º Server WS: 
		java tp2.DirServerWS -p Port -bp basePath communityName ip

 - Start 1º servidor RMI: 
		java tp2.DirServerRMI -bp basePath communityName ip

NOTE: Put the SSL X509 Certificates in the bin folder.

 - Start other servers WS: 
		java tp2.DirServerWS -p Port -bp basePath communityName urlOfServerAlreadyInTheCommunity ip

 - Start other servers RMI: 
		java tp2.DirServerRMI -bp basePath communityName urlOfServerAlreadyInTheCommunity ip
				
- Start Server Google DriveRMI: 
		java -cp .;commons-codec-1.7.jar;json-simple-1.1.1.jar;scribe-1.3.2.jar tp2.DirServerGoogleDriveRMI community ip

 - Start Server DropBoxRMI: 
		java -cp .;commons-codec-1.7.jar;json-simple-1.1.1.jar;scribe-1.3.2.jar tp2.DirServerDropBoxRMI community ip
	
	 - Start Server FlickrRMI: 
		java -cp .;commons-codec-1.7.jar;json-simple-1.1.1.jar;scribe-1.3.2.jar tp2.DirServerFlickrRMI community ip
	
	
###Client

- Start client using server automatic discovery mechanism: 
		java -cp .;json-simple-1.1.1.jar tp2.FileClient
		
 - Start client with an address from a server in a community: 
		java -cp .;json-simple-1.1.1.jar tp2.FileClient serverURL
		
NOTE: To exit from a client it's necessary to use the command "exit" in order to successfully logout from the community.