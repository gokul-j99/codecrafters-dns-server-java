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

                // Parse the question section
                var question = DnsMessage.parseQuestion(packet.getData());
                System.out.println("Parsed domain: " + question.name());
                System.out.println("Parsed QTYPE: " + question.type() + ", QCLASS: " + question.clazz());

                // Build DNS response header
                var header = DnsMessage.header(packet.getData());

                // Build the question section for the response
                var questionPacket = DnsQuestion.question(question);

                // Check if the OPCODE is supported (standard query OPCODE = 0)
                boolean supportedOpcode = question.type() == 1 && question.clazz() == 1;

                // Build the response
                byte[] response;
                if (supportedOpcode) {
                    // Supported query: Add an answer section
                    var answerPacket = DnsAnswer.answer(
                            question.name(), question.type(), question.clazz(),
                            60, new byte[]{8, 8, 8, 8} // Example IP: 8.8.8.8
                    );

                    // Combine header, question, and answer
                    response = new byte[header.length + questionPacket.length + answerPacket.length];
                    System.arraycopy(header, 0, response, 0, header.length);
                    System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                    System.arraycopy(answerPacket, 0, response, header.length + questionPacket.length, answerPacket.length);
                } else {
                    // Unsupported query: No answer section, only header and question
                    response = new byte[header.length + questionPacket.length];
                    System.arraycopy(header, 0, response, 0, header.length);
                    System.arraycopy(questionPacket, 0, response, header.length, questionPacket.length);
                }

                // Send the response
                var responsePacket = new DatagramPacket(response, response.length, packet.getSocketAddress());
                serverSocket.send(responsePacket);
                System.out.println("Sent response");
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }
}
