package StableMulticast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

public class StableMulticast {

    private final GroupMember clientGroupMember;
    private final IStableMulticast client;
    private final List<GroupMember> groupMembers;
    private final Map<GroupMember, int[]> logicalClock;
    private final Map<UUID, Message> messageBuffer;

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

        groupMembers.add(clientGroupMember);

        discoverGroupMembers();
        announcePresence();
        mreceive();
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
        final var myClock = logicalClock.getOrDefault(clientGroupMember, new int[groupMembers.size()]);

        final var message = new Message(senderAndMsg, myClock.clone(), clientGroupMember);

        Scanner in = new Scanner(System.in);

        System.out.println("Enviar a todos? (y/n)");
        boolean shouldSendToAll = in.nextLine().equals("y");

        if (!shouldSendToAll) {

            List<GroupMember> notYetSentMembers = new ArrayList<>(this.groupMembers);
            while (!notYetSentMembers.isEmpty()) {

                System.out.println("Escolha um membro para enviar a mensagem agora:");
                for (int i = 0; i < notYetSentMembers.size(); i++) {
                    GroupMember member = notYetSentMembers.get(i);
                    System.out.println((i) + ": " + member.ip() + ":" + member.port());
                }

                int selectedIndex;
                while (true) {
                    try {
                        selectedIndex = Integer.parseInt(in.nextLine().trim());
                        if (selectedIndex >= 0 && selectedIndex < notYetSentMembers.size()) {
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Formato de índice inválido");
                    }
                    System.out.println("Índice inválido");
                }

                GroupMember member = notYetSentMembers.get(selectedIndex);
                try (DatagramSocket socket = new DatagramSocket()) {
                    sendDatagram(socket, message, InetAddress.getByName(member.ip()), member.port());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                notYetSentMembers.remove(selectedIndex);
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

        myClock[getIndex(clientGroupMember)]++;
        logicalClock.put(clientGroupMember, myClock);

        System.out.println("------------------ Enviou ------------------");
        printBufferAndClock();
    }

    private synchronized void printBufferAndClock() {
        System.out.println("Buffer de Mensagens:");
        for (Map.Entry<UUID, Message> entry : messageBuffer.entrySet()) {
            System.out.println("Mensagem: " + entry.getValue().msg() +
                    " | Relógio: " + Arrays.toString(entry.getValue().clock()));
        }

        System.out.println("\nRelógio Lógico dos Processos:");
        for (Map.Entry<GroupMember, int[]> entry : logicalClock.entrySet()) {
            System.out.println("Membro: " + entry.getKey() + " | Relógio: " + Arrays.toString(entry.getValue()));
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

                    messageBuffer.put(UUID.randomUUID(), received);

                    if (!received.groupMember().equals(clientGroupMember)) {
                        logicalClock.put(received.groupMember(), received.clock());
                        int[] myClock = logicalClock.getOrDefault(clientGroupMember, new int[groupMembers.size()]);
                        int senderIndex = getIndex(received.groupMember());
                        myClock[senderIndex]++;
                        logicalClock.put(clientGroupMember, myClock);
                    }

                    client.deliver(received.msg());

                    discardStableMessages();

                    System.out.println("------------------ Recebeu ------------------");
                    printBufferAndClock();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private synchronized void discardStableMessages() {
        List<UUID> stableMessages = new ArrayList<>();

        for (Map.Entry<UUID, Message> entry : messageBuffer.entrySet()) {
            Message message = entry.getValue();
            int senderIndex = getIndex(message.groupMember());
            int msgClockValue = message.clock()[senderIndex];

            int minClockValue = Integer.MAX_VALUE;
            // menor valor no index do sender em todos os processos
            for (int[] clock : logicalClock.values()) {
                minClockValue = Math.min(minClockValue, clock[senderIndex]);
            }

            if (msgClockValue <= minClockValue) {
                stableMessages.add(entry.getKey());
            }
        }

        for (UUID key : stableMessages) {
            messageBuffer.remove(key);
            System.out.println("MIDDLEWARE: Discarded stable message: " + key);
        }
    }

    private int getIndex(GroupMember member) {
        int index = groupMembers.indexOf(member);
        if (index == -1) {
            throw new RuntimeException("MIDDLEWARE: Error - client group member not found in groupMembers");
        }
        return index;
    }
}
