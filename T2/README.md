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

# Questions

- Should we throw Remote Exception or should we handle it in a try-catch?
- Should we first register the rmi via command-line or should it be done through coding?
- How is only the server going to be responsible for closing the rooms when it does not have any method for that in the provided interface?
- What would happen if I created a room named "Servidor"? 
