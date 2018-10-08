# Oblig-1-DATS2410
First mandatory assignment in DATS 2410 - Network and cloud computing


##Implementasjon
Server based chat application. Each user connects to a server that keeps control over all active users, and corectly route messages to the correct reciver.
	
	

##Foreslåtte features
	Klienten støtter flere samtaler enn en
	Sende meldinger til bruker som er logget av (Kan føre til Gruppechat)
	Kryptering av meldinger
	Kallenavn
	Gif-implementasjon & Emjois
	Når man sender respons til noen burde de bli farget i listen, for å indikere at du har sendt en respons.
	
##SMessage Protocol (SMP)
Hver kommando starter med TYPE NUM

NUM definerer om det er en melding fra klient (1) eller en kommando (0)

Eks:
	
//Send a message (TYPE 1) to the username “Ane”. The message is “Hello”
TYPE 1
Ane
Hello

// This is sent from server to client if the login failed
//Send a command (TYPE 0) from the server to the client,
TYPE 0
LOGINFAIL

// This is sent to the server if i want to connect to Stan, or it is sent to a client if Stan want 
// to connect to that person. 
TYPE 0
CONNECT
Stan

// This is sent from the server to a client if that client asks for a complete userlist
TYPE 0
USERLIST
Ane
+
Stan
0
George
-

Første melding til server er versjon av klient

Kommandoer server skal tolke:
- REGUSER
- LOGIN
- LOGOFF
- CONNECT
- RESPONSE
- GETUSERS
- USERSTATUSUPDATE

Kommandoer klient skal tolke
- CONNECT
- RESPONSE
- DISCONNECT
- USERLIST
- LOGINFAIL
- LOGINSUCCESS
- STATUSUPDATE
- REGUSERFAIL


Status codes are specifed in the enum Status.java

GetUsers: Each username is followed with a status code, like in the following syntax example.

Syntax for commando STATUSUPDATE:

	TYPE 0
	GETUSERS
	USERNAME
	ONLINE
	USERNAME2
	OFFLINE
	USERNAME3
	BUSY

Syntax for USERSTATUSUPDATE:

	TYPE 0
	USERSTATUSUPDATE
	0||+||-

#Plan
Uke 6
- GUI
	
uke 7
- Dokumentasjon
- Ekstra
	
uke 8
- Se over program og dokumentasjon
- Ekstra

Intern deadline: 22. februar

Innlevering: Mandag uke 9
