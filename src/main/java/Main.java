import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class Main {

    public static void main(String[] args) {
        try (var serverSocket = new DatagramSocket(2053)) {
            while (true) {
                // Buffer to receive the query
                var buf = new byte[512];
                var packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                System.out.println("Received data");

                // Parse the question section
                var question = DnsMessage.parseQuestion(packet.getData());
                System.out.println("Parsed domain: " + question.name());
                System.out.println("Parsed QTYPE: " + question.type() + ", QCLASS: " + question.clazz());

                // Check for unsupported query type or class
                boolean unsupported = question.type() != 1 || question.clazz() != 1;

                // Set RCODE appropriately
                int rcode = unsupported ? 4 : 0; // 4 = NOT IMPLEMENTED, 0 = NOERROR
                int ancount = unsupported ? 0 : 1; // No answers for unsupported queries

                // Build the DNS response header
                var header = DnsMessage.headerWithAnswerCount(packet.getData(), ancount, rcode);

                // Build the question section
                var questionPacket = DnsQuestion.question(question);

                byte[] response;

                if (unsupported) {
                    // Unsupported query: Header + Question only
                    response = new byte[header.length + questionPacket.length];
                    System.arraycopy(header, 0, response, 0, header.length);
                    System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                } else {
                    // Supported query: Add the Answer section
                    var answerPacket = DnsAnswer.answer(
                            question.name(), question.type(), question.clazz(),
                            60, new byte[]{8, 8, 8, 8} // IP = 8.8.8.8
                    );

                    // Combine header, question, and answer
                    response = new byte[header.length + questionPacket.length + answerPacket.length];
                    System.arraycopy(header, 0, response, 0, header.length);
                    System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                    System.arraycopy(answerPacket, 0, response, header.length + questionPacket.length, answerPacket.length);
                }

                // Send the DNS response
                var responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response with RCODE: " + rcode);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
