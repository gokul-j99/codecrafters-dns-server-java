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

                // Parse the domain name from the request
                String domainName = extractDomainName(buf);
                System.out.println("Extracted domain name: " + domainName);

                // Crafting the DNS response
                byte[] response = createDnsResponse(requestHeader, domainName);

                // Preparing the response packet
                final DatagramPacket responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with dynamic header and question section");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static DNSHeader parseHeader(byte[] request) {
        DNSHeader header = new DNSHeader();

        header.id = (short) ((request[0] << 8) | (request[1] & 0xFF));
        header.qr = (request[2] >> 7) & 0x01;
        header.opcode = (request[2] >> 3) & 0x0F;
        header.rd = request[2] & 0x01;
        header.rcode = request[3] & 0x0F;

        return header;
    }

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

    private static byte[] createDnsResponse(DNSHeader requestHeader, String domainName) {
        // Header is 12 bytes long
        byte[] header = new byte[12];

        // Packet Identifier (ID)
        header[0] = (byte) (requestHeader.id >> 8);
        header[1] = (byte) (requestHeader.id & 0xFF);

        // QR = 1 (response), OPCODE = mimic request, AA = 0, TC = 0, RD = mimic request
        header[2] = (byte) ((1 << 7) | (requestHeader.opcode << 3) | (0 << 2) | (0 << 1) | requestHeader.rd);

        // RA = 0, Z = 0, RCODE = 0 (no error)
        header[3] = (byte) ((0 << 7) | (0 << 4) | 0);

        // QDCOUNT = 1
        header[4] = 0x00;
        header[5] = 0x01;

        // ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0 (no answers for now)
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

    private static byte[] encodeDomainName(String domainName) {
        String[] labels = domainName.split("\\.");
        byte[] encoded = new byte[domainName.length() + 2]; // Add 2 for label lengths and null terminator
        int index = 0;

        for (String label : labels) {
            encoded[index++] = (byte) label.length();
            for (char c : label.toCharArray()) {
                encoded[index++] = (byte) c;
            }
        }
        encoded[index] = 0x00; // Null byte to terminate the domain name
        return encoded;
    }

    static class DNSHeader {
        short id;
        int qr;
        int opcode;
        int rd;
        int rcode;
    }
}
