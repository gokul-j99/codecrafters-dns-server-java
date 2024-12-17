import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {

    public static void main(String[] args) {
        try (var serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Buffer to receive the incoming query
                var buf = new byte[512];
                var packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Parse the question section
                var question = DnsMessage.parseQuestion(packet.getData());
                System.out.println("Parsed domain: " + question.name());
                System.out.println("Parsed QTYPE: " + question.type() + ", QCLASS: " + question.clazz());

                // Build DNS response header
                var header = DnsMessage.header(packet.getData());

                // Build the question section for the response
                var questionPacket = DnsQuestion.question(question);

                // Build the answer section
                var answerPacket = DnsAnswer.answer(
                        question.name(),      // Domain name
                        question.type(),      // QTYPE
                        question.clazz(),     // QCLASS
                        60,                   // TTL
                        new byte[]{8, 8, 8, 8} // Example IP: 8.8.8.8
                );

                // Combine header, question, and answer sections
                var response = new byte[header.length + questionPacket.length + answerPacket.length];
                System.arraycopy(header, 0, response, 0, header.length);
                System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                System.arraycopy(answerPacket, 0, response, header.length + questionPacket.length, answerPacket.length);

                // Send the response
                var responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with answer section");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
