import static java.lang.System.arraycopy;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {
    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(2053)) {
            while (true) {
                final byte[] buf = new byte[512];
                final DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                final var header = DnsMessage.header(buf);
                final var questionPacket = DnsQuestion.question("codecrafters.io", 1, 1); // QTYPE=1, QCLASS=1
                final var answerPacket = DnsAnswer.answer("codecrafters.io", 1, 1); // A record, Internet class

                final var bufResponse = new byte[header.length + questionPacket.length + answerPacket.length];
                arraycopy(header, 0, bufResponse, 0, header.length);
                arraycopy(questionPacket, 0, bufResponse, header.length, questionPacket.length);
                arraycopy(answerPacket, 0, bufResponse, header.length + questionPacket.length, answerPacket.length);

                final DatagramPacket packetResponse = new DatagramPacket(
                        bufResponse, bufResponse.length, packet.getSocketAddress());
                serverSocket.send(packetResponse);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
