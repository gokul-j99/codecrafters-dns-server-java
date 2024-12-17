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

                // Build DNS response header (ANCOUNT = 1 for 1 answer)
                var header = DnsMessage.headerWithAnswerCount(packet.getData(), 1);

                // Build the question section for the response
                var questionPacket = DnsQuestion.question(question);

                // Build the answer section
                var answerPacket = DnsAnswer.answer(
                        question.name(),      // Domain name
                        question.type(),      // QTYPE = 1
                        question.clazz(),     // QCLASS = 1
                        60,                   // TTL = 60 seconds
                        new byte[]{8, 8, 8, 8} // RDATA = 8.8.8.8
                );

                // Combine header, question, and answer sections
                int responseSize = header.length + questionPacket.length + answerPacket.length;
                var response = new byte[responseSize];
                System.arraycopy(header, 0, response, 0, header.length);
                System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                System.arraycopy(answerPacket, 0, response, header.length + questionPacket.length, answerPacket.length);

                // Send the response packet
                var responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with answer section");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
