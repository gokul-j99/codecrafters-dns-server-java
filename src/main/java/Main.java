import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Main {

    private static final int MAX_DNS_PACKET_SIZE = 512;

    public static void main(String[] args) {
        try (var serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Receive packet
                byte[] buf = new byte[MAX_DNS_PACKET_SIZE];
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
                    questions.add(readCompressedDomainName(ByteBuffer.wrap(buf)));
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

                // Validate response size
                byte[] responseData = response.toByteArray();
                if (responseData.length > MAX_DNS_PACKET_SIZE) {
                    System.err.println("Error: Response size exceeds 512 bytes. Truncating.");
                    responseData = truncateResponse(responseData, MAX_DNS_PACKET_SIZE);
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

    private static String readCompressedDomainName(ByteBuffer buffer) {
        byte labelLength;
        StringJoiner labels = new StringJoiner(".");

        boolean compressed = false;
        int position = 0;

        while ((labelLength = buffer.get()) != 0) {
            if ((labelLength & 0xC0) == 0xC0) {
                compressed = true;
                // Handle pointer compression
                int offset = ((labelLength & 0x3F) << 8) | (buffer.get() & 0xFF);
                position = buffer.position();
                buffer.position(offset);
            } else {
                byte[] label = new byte[labelLength];
                buffer.get(label);
                labels.add(new String(label));
            }
        }

        if (compressed) {
            buffer.position(position);
        }

        return labels.toString();
    }

    // Helper function to truncate response
    private static byte[] truncateResponse(byte[] responseData, int maxLength) {
        byte[] truncated = new byte[maxLength];
        System.arraycopy(responseData, 0, truncated, 0, maxLength);
        return truncated;
    }
}
