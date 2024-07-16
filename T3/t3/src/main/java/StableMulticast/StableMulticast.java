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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.*;

public class StableMulticast {

    private final GroupMember clientGroupMember;
    private final IStableMulticast client;
    private final List<GroupMember> groupMembers;
    private final Map<String, String> messageBuffer;
    private final Map<String, int[]> logicalClock;

    public static final String MULTICAST_IP = "224.0.0.1";
    public static final Integer MULTICAST_PORT = 4446;

    private final CountDownLatch latch = new CountDownLatch(1);

    static class GroupMember implements Serializable {
        private final String ip;
        private final Integer port;

        public GroupMember(String ip, Integer port) {
            this.ip = ip;
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public Integer getPort() {
            return port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GroupMember that = (GroupMember) o;
            return Objects.equals(ip, that.ip) && Objects.equals(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hash(ip, port);
        }

        @Override
        public String toString() {
            return "GroupMember[ip=" + ip + ", port=" + port + "]";
        }
    }

    public StableMulticast(String ip, Integer port, IStableMulticast client) {
        this.clientGroupMember = new GroupMember(ip, port);
        this.client = client;
        this.groupMembers = new ArrayList<>();
        this.messageBuffer = new HashMap<>();
        this.logicalClock = new HashMap<>();

        // Add the client itself to the list of group members
        groupMembers.add(clientGroupMember);

        discoverGroupMembers();
        announcePresence();
        mreceive();
    }

    private void discoverGroupMembers() {
        new Thread(() -> {
            byte[] buffer = new byte[256];

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
                try (MulticastSocket multicastSocket = new MulticastSocket();
                     ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {

                    oos.writeObject(new GroupMember(clientGroupMember.getIp(), clientGroupMember.getPort()));
                    byte[] message = bos.toByteArray();

                    InetAddress group = InetAddress.getByName(MULTICAST_IP);

                    DatagramPacket packet = new DatagramPacket(message, message.length, group, MULTICAST_PORT);
                    multicastSocket.send(packet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void msend(String msg) {
        if (groupMembers.isEmpty()) {
            System.out.println("MIDDLEWARE: No group members available to send the message.");
            return;
        }

        msg = this.clientGroupMember.toString() + ": " + msg;

        // constroi o timestamp do relogio
        int[] myClock = logicalClock.getOrDefault(this.clientGroupMember.getIp(), new int[groupMembers.size()]);
        int myIndex = getIndex(this.clientGroupMember);
        if (myIndex != -1) {
            myClock[myIndex]++;
        } else {
            System.err.println("MIDDLEWARE: Error - client group member not found in groupMembers.");
            return;
        }
        logicalClock.put(this.clientGroupMember.getIp(), myClock);

        msg = msg + "|" + Arrays.toString(myClock) + "|" + this.clientGroupMember.getIp();
        // envia a mensagem para todos os membros do grupo
        for (GroupMember member : groupMembers) {
            sendUnicast(msg, member);
        }
    }

    private void sendUnicast(String msg, GroupMember member) {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] message = msg.getBytes();
            InetAddress address = InetAddress.getByName(member.getIp());
            DatagramPacket packet = new DatagramPacket(message, message.length, address, member.getPort());
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mreceive() {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket(clientGroupMember.getPort())) {
                byte[] buffer = new byte[256];

                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String receivedMsg = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = receivedMsg.split("\\|");
                    String message = parts[0];
                    String clockString = parts[1];
                    String sender = parts[2];

                    int[] senderClock = parseClock(clockString);

                    // Update local vector clock with sender's clock
                    logicalClock.put(sender, senderClock);

                    // Deposit message into the buffer
                    messageBuffer.put(UUID.randomUUID().toString(), receivedMsg);

                    // Update received message counter if sender is different
                    if (!sender.equals(clientGroupMember.getIp())) {
                        int[] myClock = logicalClock.getOrDefault(clientGroupMember.getIp(), new int[groupMembers.size()]);
                        int senderIndex = getMemberIndex(sender);
                        if (senderIndex != -1) {
                            myClock[senderIndex]++;
                            logicalClock.put(clientGroupMember.getIp(), myClock);
                        }
                    }

                    // Deliver message to the upper layer
                    client.deliver(message);

                    // Check for stable messages to discard
                    discardStableMessages();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }


    private int[] parseClock(String clockString) {
        clockString = clockString.replace("[", "").replace("]", "").replace(" ", "");
        String[] parts = clockString.split(",");
        int[] clock = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            clock[i] = Integer.parseInt(parts[i]);
        }
        return clock;
    }

    private int getIndex(GroupMember member) {
        return groupMembers.indexOf(member);
    }

    private int getMemberIndex(String ip) {
        for (int i = 0; i < groupMembers.size(); i++) {
            if (groupMembers.get(i).getIp().equals(ip)) {
                return i;
            }
        }
        return -1;
    }

    private void discardStableMessages() {
        for (Map.Entry<String, String> entry : messageBuffer.entrySet()) {
            String message = entry.getValue();
            String[] parts = message.split("\\|");
            String clockString = parts[1];
            int[] msgClock = parseClock(clockString);
            String sender = parts[2];

            int senderIndex = getMemberIndex(sender);

            if (senderIndex == -1) continue; // Skip if the sender is not found

            boolean stable = true;
            for (int[] clock : logicalClock.values()) {
                if (msgClock[senderIndex] > clock[senderIndex]) {
                    stable = false;
                    break;
                }
            }

            if (stable) {
                messageBuffer.remove(entry.getKey());
                System.out.println("MIDDLEWARE: Discarded stable message: " + entry.getKey());
            }
        }
    }

}
