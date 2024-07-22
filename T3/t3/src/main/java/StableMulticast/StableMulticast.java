package StableMulticast;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class StableMulticast {

    private final GroupMember clientGroupMember;
    private final IStableMulticast client;
    private final List<GroupMember> groupMembers;
    private final Map<Message, Message> messageBuffer;
    private final Map<String, int[]> logicalClock;
    private final Map<Message, List<Integer>> delayedMessages;

    public static final String MULTICAST_IP = "224.0.0.1";
    public static final Integer MULTICAST_PORT = 4446;
    public static final Integer BUFFER_SIZE = 1024;

    private final CountDownLatch latch = new CountDownLatch(1);

    record GroupMember(String ip, Integer port) implements Serializable {
    }

    record Message(String msg, int[] clock, GroupMember groupMember) implements Serializable {
    }

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.clientGroupMember = new GroupMember(ip, port);
        this.client = client;
        this.groupMembers = new ArrayList<>();
        this.messageBuffer = new HashMap<>();
        this.logicalClock = new HashMap<>();
        this.delayedMessages = new HashMap<>();

        groupMembers.add(clientGroupMember);

        discoverGroupMembers();
        announcePresence();
        mreceive();
        //monitorUserInput();
    }

    private void discoverGroupMembers() {
        new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];

            try (MulticastSocket multicastSocket = new MulticastSocket(MULTICAST_PORT)) {
                InetAddress group = InetAddress.getByName(MULTICAST_IP);
                NetworkInterface netIf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
                multicastSocket.joinGroup(groupAddress, netIf);

                latch.countDown();

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);

                    try (ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                         ObjectInputStream ois = new ObjectInputStream(bis)) {

                        GroupMember received = (GroupMember) ois.readObject();

                        synchronized (groupMembers) {
                            if (!received.equals(clientGroupMember) && !groupMembers.contains(received)) {
                                groupMembers.add(received);
                                System.out.println("MIDDLEWARE: New group member: " + received);
                                announcePresence();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void announcePresence() {
        new Thread(() -> {
            try {
                latch.await();

                try (MulticastSocket multicastSocket = new MulticastSocket()) {
                    GroupMember groupMember = new GroupMember(clientGroupMember.ip(), clientGroupMember.port());
                    sendDatagram(multicastSocket, groupMember, InetAddress.getByName(MULTICAST_IP), MULTICAST_PORT);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void msend(final String msg) {
        if (groupMembers.isEmpty()) {
            System.out.println("MIDDLEWARE: No group members available to send the message.");
            return;
        }

        final var senderAndMsg = clientGroupMember.toString() + ": " + msg;

        int[] myClock = logicalClock.getOrDefault(clientGroupMember.ip(), new int[groupMembers.size()]);
        myClock[getIndex(clientGroupMember)]++;
        logicalClock.put(clientGroupMember.ip(), myClock);

        // Clone the clock to ensure uniqueness
        final var message = new Message(senderAndMsg, myClock.clone(), clientGroupMember);

        Scanner in = new Scanner(System.in);

        System.out.println("Enviar a todos? (y/n)");
        Boolean shouldSendToAll = in.nextLine().equals("y");

        if (!shouldSendToAll) {
            System.out.println("Escolha os membros para enviar a mensagem (digite o número correspondente, separados por vírgula):");
            for (int i = 0; i < groupMembers.size(); i++) {
                GroupMember member = groupMembers.get(i);
                System.out.println((i + 1) + ": " + member.ip() + ":" + member.port());
            }

            String[] selectedIndices = in.nextLine().split(",");
            List<Integer> selectedMembers = new ArrayList<>();
            for (String index : selectedIndices) {
                try {
                    int memberIndex = Integer.parseInt(index.trim()) - 1;
                    if (memberIndex >= 0 && memberIndex < groupMembers.size()) {
                        selectedMembers.add(memberIndex);
                    } else {
                        System.out.println("Índice inválido: " + (memberIndex + 1));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Formato de índice inválido: " + index);
                }
            }
            System.out.println("Enviar agora? (y/n)");
            Boolean sendNow = in.nextLine().equals("y");

            if (sendNow) {
                for (int index : selectedMembers) {
                    GroupMember member = groupMembers.get(index);
                    try (DatagramSocket socket = new DatagramSocket()) {
                        sendDatagram(socket, message, InetAddress.getByName(member.ip()), member.port());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                delayedMessages.put(message, selectedMembers);
                messageBuffer.put(message, message);
                System.out.println("MIDDLEWARE: Mensagem armazenada para envio posterior. Pressione 'e' para enviar todas as mensagens atrasadas.");
            }

        } else {
            for (GroupMember member : groupMembers) {
                try (DatagramSocket socket = new DatagramSocket()) {
                    sendDatagram(socket, message, InetAddress.getByName(member.ip()), member.port());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        printBufferAndClock();
    }

    private void printBufferAndClock() {
        System.out.println("Buffer de Mensagens:");
        for (Map.Entry<Message, Message> entry : messageBuffer.entrySet()) {
            System.out.println("Mensagem: " + entry.getValue().msg() +
                    " | Relógio: " + Arrays.toString(entry.getValue().clock()));
        }

        System.out.println("\nRelógio Lógico dos Processos:");
        for (Map.Entry<String, int[]> entry : logicalClock.entrySet()) {
            System.out.println("IP: " + entry.getKey() + " | Relógio: " + Arrays.toString(entry.getValue()));
        }
    }

    private void sendDatagram(DatagramSocket socket, Object content, InetAddress address, int port) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(content);
            byte[] message = bos.toByteArray();

            DatagramPacket packet = new DatagramPacket(message, message.length, address, port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mreceive() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(clientGroupMember.port())) {
                byte[] buffer = new byte[BUFFER_SIZE];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    Message received;

                    try (ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData());
                         ObjectInputStream ois = new ObjectInputStream(bis)) {

                        received = (Message) ois.readObject();
                    }

                    // Update messageBuffer and logicalClock
                    messageBuffer.put(received, received);
                    logicalClock.put(received.groupMember().ip(), received.clock());

                    if (!received.groupMember().equals(clientGroupMember)) {
                        int[] myClock = logicalClock.getOrDefault(clientGroupMember.ip(), new int[groupMembers.size()]);
                        int senderIndex = getIndex(received.groupMember());
                        myClock[senderIndex]++;
                        logicalClock.put(clientGroupMember.ip(), myClock);
                    }

                    // Sort messages by logical clock
                    List<Map.Entry<Message, Message>> sortedMessages = new ArrayList<>(messageBuffer.entrySet());
                    sortedMessages.sort((entry1, entry2) -> {
                        Message msg1 = entry1.getValue();
                        Message msg2 = entry2.getValue();
                        return compareLogicalClocks(msg1.clock(), msg2.clock());
                    });

                    for (Map.Entry<Message, Message> entry : sortedMessages) {
                        client.deliver(entry.getValue().msg());
                    }

                    discardStableMessages();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private int compareLogicalClocks(int[] clock1, int[] clock2) {
        for (int i = 0; i < clock1.length; i++) {
            if (clock1[i] < clock2[i]) {
                return -1;
            } else if (clock1[i] > clock2[i]) {
                return 1;
            }
        }
        return 0;
    }

    private void discardStableMessages() {
        List<Message> stableMessages = new ArrayList<>();

        for (Map.Entry<Message, Message> entry : messageBuffer.entrySet()) {
            Message message = entry.getValue();

            int senderIndex = getIndex(message.groupMember());

            boolean stable = true;
            for (int[] clock : logicalClock.values()) {
                if (message.clock()[senderIndex] > clock[senderIndex]) {
                    stable = false;
                    break;
                }
            }

            if (stable) {
                stableMessages.add(entry.getKey());
            }
        }

        for (Message key : stableMessages) {
            messageBuffer.remove(key);
            System.out.println("MIDDLEWARE: Discarded stable message: " + key.msg());
        }
    }

    private int getIndex(GroupMember member) {
        int index = groupMembers.indexOf(member);
        if (index == -1) {
            throw new RuntimeException("MIDDLEWARE: Error - client group member not found in groupMembers");
        }
        return index;
    }

    private void sendToRecipients(Message message, List<Integer> selectedMembers) {
        for (int index : selectedMembers) {
            GroupMember member = groupMembers.get(index);
            try (DatagramSocket socket = new DatagramSocket()) {
                sendDatagram(socket, message, InetAddress.getByName(member.ip()), member.port());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDelayedMessages() {
        for (Map.Entry<Message, List<Integer>> entry : delayedMessages.entrySet()) {
            sendToRecipients(entry.getKey(), entry.getValue());
        }
        delayedMessages.clear();
        System.out.println("MIDDLEWARE: Todas as mensagens atrasadas foram enviadas.");
    }

    private void monitorUserInput() {
        new Thread(() -> {
            Scanner in = new Scanner(System.in);
            while (true) {
                String command = in.nextLine();
                if (command.equals("e")) {
                    sendDelayedMessages();
                }
            }
        }).start();
    }
}
