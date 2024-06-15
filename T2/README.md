# Compile & Run

On Linux, from the source-code folder:

1. 
``` bash 
javac IRoomChat.java IServerChat.java IUserChat.java RoomChat.java ServerChat.java UserChat.java 
```
2. 
``` bash 
rmiregistry 2020 &
```
3. 
``` bash 
java ServerChat
```
4. 
``` bash 
java RoomChat
```
# Kill rmiregistry

``` bash 
pkill rmiregistry
```