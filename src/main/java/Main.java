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
                byte[] header = DnsMessage.headerWithAnswerCount(buf, unsupported ? 0 : questions.size(), unsupported ? 4 : 0);
                output.write(header);

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

                // Ensure response size is within the limit
                byte[] responseData = response.toByteArray();
                if (responseData.length > 512) {
                    System.err.println("Error: Response size exceeds 512 bytes. Truncating.");
                    responseData = truncateResponse(responseData, 512);
                }

                // Send response
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with RCODE: " + (unsupported ? 4 : 0));
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
        }
    }

    // Helper function to truncate response
    private static byte[] truncateResponse(byte[] responseData, int maxLength) {
        byte[] truncated = new byte[maxLength];
        System.arraycopy(responseData, 0, truncated, 0, maxLength);
        return truncated;
    }
}
