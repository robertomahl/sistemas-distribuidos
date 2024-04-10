# T1

## DateServer.java

This is perhaps the simplest possible server. It listens on port 59090. When a client connects, the server sends the current datetime to the client. The connection socket is created in a try-with-resources block so it is automatically closed at the end of the block. Only after serving the datetime and closing the connection will the server go back to waiting for the next client.

- This code is just for illustration; you are unlikely to ever write anything so simple.
- This does not handle multiple clients well; each client must wait until the previous client is completely served before it even gets accepted.
- As in virtually all socket programs, a server socket just listens, and a different, “plain” socket communicates with the client.
T- he ServerSocket.accept() call is a BLOCKING CALL.
- Socket communication is always with bytes; therefore sockets come with input streams and output streams. But by wrapping the socket’s output stream with a PrintWriter, we can specify strings to write, which Java will automatically convert (decode) to bytes.
- Communication through sockets is always buffered. This means nothing is sent or received until the buffers fill up, or you explicitly flush the buffer. The second argument to the PrintWriter, in this case true tells Java to flush automatically after every println.
- We defined all sockets in a try-with-resources block so they will automatically close at the end of their block. No explicit close call is required.
- After sending the datetime to the client, the try-block ends and the communication socket is closed, so in this case, closing the connection is initiated by the server.

```bash
java DateServer.java
netstat -an | grep 59090
nc localhost 59090
```
