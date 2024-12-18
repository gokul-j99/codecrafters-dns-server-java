import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Receive packet
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Parse the DNS header and question section
                DataInputStream input = new DataInputStream(new ByteArrayInputStream(buf));
                short id = input.readShort();
                short flags = input.readShort();
                short qdCount = input.readShort();
                input.readShort(); // ANCOUNT
                input.readShort(); // NSCOUNT
                input.readShort(); // ARCOUNT

                List<String> questions = new ArrayList<>();
                for (int i = 0; i < qdCount; i++) {
                    questions.add(DnsMessage.readDomainName(input, buf));
                    input.readShort(); // QTYPE
                    input.readShort(); // QCLASS
                }

                // Debug parsed domains
                for (String domain : questions) {
                    System.out.println("Parsed domain: " + domain);
                }

                // Build response
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(response);

                // Header
                output.writeShort(id); // ID
                output.writeShort(0x8180); // Flags: QR=1, RD=1, RA=0, NOERROR
                output.writeShort(qdCount); // QDCOUNT
                output.writeShort(qdCount); // ANCOUNT = same as questions count
                output.writeShort(0); // NSCOUNT
                output.writeShort(0); // ARCOUNT

                // Question section
                for (String domain : questions) {
                    output.write(DnsMessage.encodeDomain(domain));
                    output.writeShort(1); // QTYPE=A
                    output.writeShort(1); // QCLASS=IN
                }

                // Answer section (dummy A records)
                for (String domain : questions) {
                    output.write(DnsMessage.encodeDomain(domain));
                    output.writeShort(1); // TYPE=A
                    output.writeShort(1); // CLASS=IN
                    output.writeInt(60); // TTL
                    output.writeShort(4); // RDLENGTH
                    output.write(new byte[]{8, 8, 8, 8}); // Dummy IP 8.8.8.8
                }

                // Send response
                byte[] responseData = response.toByteArray();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with answers");
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }
}
