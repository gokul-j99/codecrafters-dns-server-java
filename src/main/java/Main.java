import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
    public static void main(String[] args) {
        System.out.println("DNS Server is running...");

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Buffer to receive the incoming packet
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received query");

                // Parse the domain name from the request
                String domainName = extractDomainName(buf);
                System.out.println("Extracted domain name: " + domainName);

                // Crafting the DNS response header + question
                byte[] response = createDnsResponse(domainName);

                // Preparing the response packet
                final DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with question section");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Extracts the domain name from the DNS query packet.
     * @param request The byte array representing the DNS query packet.
     * @return The extracted domain name.
     */
    private static String extractDomainName(byte[] request) {
        StringBuilder domainName = new StringBuilder();
        int index = 12; // Domain name starts after the 12-byte header

        while (request[index] != 0) {
            int labelLength = request[index++];
            for (int i = 0; i < labelLength; i++) {
                domainName.append((char) request[index++]);
            }
            if (request[index] != 0) {
                domainName.append(".");
            }
        }

        return domainName.toString();
    }

    /**
     * Creates a DNS response including the header and question section.
     * @param domainName The domain name to include in the question section.
     * @return The byte array representing the DNS response.
     */
    private static byte[] createDnsResponse(String domainName) {
        // Header is 12 bytes long
        byte[] header = new byte[12];

        // Header fields
        header[0] = 0x04; // ID high byte (0x04D2 -> 1234)
        header[1] = (byte) 0xD2; // ID low byte
        header[2] = (byte) 0b10000000; // QR = 1 (response), OPCODE = 0, AA = 0, TC = 0, RD = 0
        header[3] = 0x00; // RA = 0, Z = 0, RCODE = 0
        header[4] = 0x00; // QDCOUNT high byte (1 question)
        header[5] = 0x01; // QDCOUNT low byte
        header[6] = 0x00; // ANCOUNT high byte
        header[7] = 0x00; // ANCOUNT low byte
        header[8] = 0x00; // NSCOUNT high byte
        header[9] = 0x00; // NSCOUNT low byte
        header[10] = 0x00; // ARCOUNT high byte
        header[11] = 0x00; // ARCOUNT low byte

        // Question section: Name + Type + Class
        byte[] question = encodeDomainName(domainName);
        byte[] typeAndClass = new byte[4];
        typeAndClass[0] = 0x00; // Type high byte (A record = 1)
        typeAndClass[1] = 0x01; // Type low byte
        typeAndClass[2] = 0x00; // Class high byte (IN = 1)
        typeAndClass[3] = 0x01; // Class low byte

        // Combine header and question section
        byte[] response = new byte[header.length + question.length + typeAndClass.length];
        System.arraycopy(header, 0, response, 0, header.length);
        System.arraycopy(question, 0, response, header.length, question.length);
        System.arraycopy(typeAndClass, 0, response, header.length + question.length, typeAndClass.length);

        return response;
    }

    /**
     * Encodes a domain name into the DNS label format.
     * @param domainName The domain name to encode.
     * @return The byte array representing the encoded domain name.
     */
    private static byte[] encodeDomainName(String domainName) {
        String[] labels = domainName.split("\\.");
        byte[] encoded = new byte[domainName.length() + 2]; // Add 2 for label lengths and null terminator
        int index = 0;

        for (String label : labels) {
            encoded[index++] = (byte) label.length(); // Length of the label
            for (char c : label.toCharArray()) {
                encoded[index++] = (byte) c; // Content of the label
            }
        }
        encoded[index] = 0x00; // Null byte to terminate the domain name
        return encoded;
    }
}
