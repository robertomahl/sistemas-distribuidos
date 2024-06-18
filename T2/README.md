Luana e Roberto

# Compilação & Execução

Em ambiente Linux, a partir do diretório do código-fonte:

0. Defina o seu IP público na constante SERVER_IP_ADDRESS, em UserChat.java e ServerChat.java.
1. 
``` bash 
javac IRoomChat.java IServerChat.java IUserChat.java RoomChat.java ServerChat.java UserChat.java 
```
2. 
``` bash 
java ServerChat
```
3.
``` bash 
java UserChat <nome_usuario>
```

# Uso

Servidor:
- Apenas a lista de salas é exibida;
- Ao clicar sobre uma lista ela é fechada.

Clientes:
- Para criar uma sala, clique em "Add room";
- Na coluna à esquerda, selecione uma sala para entrar;
- Para sair da sala, use o botão "Leave";
- Para enviar uma mensagem, escreva no campo sob as mensagens e pressione enter para enviar.