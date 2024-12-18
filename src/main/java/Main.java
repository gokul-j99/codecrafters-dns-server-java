import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try (var serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Receive packet
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Parse OPCODE and question section
                int opcode = DnsMessage.getOpcode(buf);
                System.out.println("Received OPCODE: " + opcode);

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

                // Handle unsupported OPCODEs
                boolean unsupported = opcode != 0;

                // Build response
                ByteArrayOutputStream response = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(response);

                // Header
                output.writeShort(id); // ID
                output.writeShort(unsupported ? 0x8184 : 0x8180); // Flags: QR=1, RA=0, RCODE=4 for unsupported
                output.writeShort(qdCount); // QDCOUNT
                output.writeShort(unsupported ? 0 : qdCount); // ANCOUNT = 0 if unsupported
                output.writeShort(0); // NSCOUNT
                output.writeShort(0); // ARCOUNT

                // Question section
                for (String domain : questions) {
                    output.write(DnsMessage.encodeDomain(domain));
                    output.writeShort(1); // QTYPE=A
                    output.writeShort(1); // QCLASS=IN
                }

                // Answer section (only for supported OPCODE)
                if (!unsupported) {
                    for (String domain : questions) {
                        output.write(DnsMessage.encodeDomain(domain));
                        output.writeShort(1); // TYPE=A
                        output.writeShort(1); // CLASS=IN
                        output.writeInt(60); // TTL
                        output.writeShort(4); // RDLENGTH
                        output.write(new byte[]{8, 8, 8, 8}); // Dummy IP 8.8.8.8
                    }
                }

                // Send response
                byte[] responseData = response.toByteArray();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with RCODE: " + (unsupported ? 4 : 0));
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }
}
