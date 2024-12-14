import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Buffer to receive the incoming packet
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Crafting the DNS response header
                byte[] header = new byte[12];

                // Setting up header fields
                // Packet Identifier (ID): 1234 (0x04D2 in hexadecimal)
                header[0] = 0x04; // High byte
                header[1] = (byte) 0xD2; // Low byte

                // QR: 1, OPCODE: 0, AA: 0, TC: 0, RD: 0 (bitwise packing into 1 byte)
                header[2] = (byte) 0b10000000;

                // RA: 0, Z: 0, RCODE: 0 (bitwise packing into 1 byte)
                header[3] = 0x00;

                // QDCOUNT: 0 (2 bytes)
                header[4] = 0x00;
                header[5] = 0x00;

                // ANCOUNT: 0 (2 bytes)
                header[6] = 0x00;
                header[7] = 0x00;

                // NSCOUNT: 0 (2 bytes)
                header[8] = 0x00;
                header[9] = 0x00;

                // ARCOUNT: 0 (2 bytes)
                header[10] = 0x00;
                header[11] = 0x00;

                // Preparing the response packet
                final DatagramPacket responsePacket = new DatagramPacket(header, header.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
