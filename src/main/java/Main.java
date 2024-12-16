import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Main {

    public static void main(String[] args) {
        try (var serverSocket = new DatagramSocket(2053)) {
            while (true) {
                var buf = new byte[512];
                var packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Parse question
                var question = DnsMessage.parseQuestion(buf);

                // Build DNS response
                var header = DnsMessage.header(buf);
                var questionPacket = DnsQuestion.question(question);
                var answerPacket = DnsAnswer.answer(question.name(), question.type(), question.clazz(), 60, new byte[]{8, 8, 8, 8}); // IP = 8.8.8.8

                // Combine all parts into the response
                var response = new byte[header.length + questionPacket.length + answerPacket.length];
                System.arraycopy(header, 0, response, 0, header.length);
                System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                System.arraycopy(answerPacket, 0, response, header.length + questionPacket.length, answerPacket.length);

                // Send response
                var responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
