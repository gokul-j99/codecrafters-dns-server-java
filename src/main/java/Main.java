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

                // Parse the DNS header from the request
                DNSHeader requestHeader = parseHeader(buf);
                System.out.println("Parsed Header: ID=" + requestHeader.id);

                // Crafting the DNS response
                byte[] response = createDnsResponse(requestHeader);

                // Preparing the response packet
                final DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with dynamic header");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    /**
     * Parses the header of the DNS request packet.
     * @param request The byte array representing the DNS query packet.
     * @return A DNSHeader object containing the parsed values.
     */
    private static DNSHeader parseHeader(byte[] request) {
        DNSHeader header = new DNSHeader();

        // Packet ID
        header.id = (short) ((request[0] << 8) | (request[1] & 0xFF));

        // QR, OPCODE, AA, TC, RD
        header.qr = (request[2] >> 7) & 0x01;
        header.opcode = (request[2] >> 3) & 0x0F;
        header.rd = request[2] & 0x01;

        // RA, Z, RCODE
        header.rcode = request[3] & 0x0F;

        return header;
    }

    /**
     * Creates a DNS response including a dynamic header based on the request.
     * @param requestHeader The header parsed from the DNS request.
     * @return The byte array representing the DNS response.
     */
    private static byte[] createDnsResponse(DNSHeader requestHeader) {
        // Header is 12 bytes long
        byte[] header = new byte[12];

        // Packet Identifier (ID) - Mimic the ID from the request
        header[0] = (byte) (requestHeader.id >> 8); // High byte
        header[1] = (byte) (requestHeader.id & 0xFF); // Low byte

        // QR = 1 (response), mimic OPCODE, AA = 0, TC = 0, mimic RD
        header[2] = (byte) ((1 << 7) | (requestHeader.opcode << 3) | (0 << 2) | (0 << 1) | requestHeader.rd);

        // RA = 0, Z = 0, RCODE = 0 (if standard query), else 4 (not implemented)
        int rcode = (requestHeader.opcode == 0) ? 0 : 4;
        header[3] = (byte) ((0 << 7) | (0 << 4) | rcode);

        // QDCOUNT, ANCOUNT, NSCOUNT, ARCOUNT (set to 1 for simplicity)
        header[4] = 0x00; // QDCOUNT high byte
        header[5] = 0x01; // QDCOUNT low byte
        header[6] = 0x00; // ANCOUNT high byte
        header[7] = 0x01; // ANCOUNT low byte
        header[8] = 0x00; // NSCOUNT high byte
        header[9] = 0x00; // NSCOUNT low byte
        header[10] = 0x00; // ARCOUNT high byte
        header[11] = 0x00; // ARCOUNT low byte

        // Return the response header
        return header;
    }

    /**
     * A helper class to store DNS header values.
     */
    static class DNSHeader {
        short id;    // Packet ID
        int qr;      // Query/Response Indicator
        int opcode;  // Operation Code
        int rd;      // Recursion Desired
        int rcode;   // Response Code
    }
}
