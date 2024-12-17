import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DnsMessage {

    private static final int RD = 1 << 8; // Recursion Desired
    private static final int RCODE = 0;  // No Error


    public static byte[] headerWithAnswerCount(byte[] received, int answerCount, int rcode) throws IOException {
        try (var inputStream = new ByteArrayInputStream(received);
             var dataInputStream = new DataInputStream(inputStream)) {

            final short id = dataInputStream.readShort();
            final short receivedFlags = dataInputStream.readShort();

            int responseFlags = 1 << 15; // QR = 1 (Response)
            int opcode = (receivedFlags >> 11) & 0x0F;
            responseFlags |= opcode << 11; // Preserve OPCODE
            responseFlags |= receivedFlags & 0x0100; // Preserve RD
            responseFlags |= rcode; // Set RCODE (0 or 4)

            return ByteBuffer.allocate(12)
                    .order(BIG_ENDIAN)
                    .putShort(id) // ID
                    .putShort((short) responseFlags) // Flags
                    .putShort((short) 1) // QDCOUNT
                    .putShort((short) answerCount) // ANCOUNT (1 for valid, 0 otherwise)
                    .putShort((short) 0) // NSCOUNT
                    .putShort((short) 0) // ARCOUNT
                    .array();
        }
    }






    public static Question parseQuestion(byte[] received) throws IOException {
        var inputStream = new ByteArrayInputStream(received, 12, received.length - 12); // Start at question section
        var domainName = new StringBuilder();
        int length;

        while ((length = inputStream.read()) > 0) {
            var label = new byte[length];
            inputStream.read(label);
            domainName.append(new String(label, UTF_8)).append(".");
        }
        domainName.setLength(domainName.length() - 1); // Remove trailing dot

        // Read QTYPE and QCLASS
        var qtype = (short) ((inputStream.read() << 8) | inputStream.read());
        var qclass = (short) ((inputStream.read() << 8) | inputStream.read());

        return new Question(domainName.toString(), qtype, qclass);
    }

    public static byte[] encodeDomain(String domain) throws IOException {
        var output = new ByteArrayOutputStream();
        for (var label : domain.split("\\.")) {
            output.write(label.length());
            output.write(label.getBytes(UTF_8));
        }
        output.write(0); // Null byte terminator
        return output.toByteArray();
    }

    public static boolean isUnsupportedQuery(short flags) {
        int opcode = (flags >> 11) & 0x0F; // Extract OPCODE (bits 11-14)
        return opcode != 0; // Unsupported if OPCODE is not 0 (standard query)
    }


    public static int getOpcode(byte[] received) throws IOException {
        try (var inputStream = new ByteArrayInputStream(received);
             var dataInputStream = new DataInputStream(inputStream)) {
            dataInputStream.readShort(); // Skip ID
            short flags = dataInputStream.readShort();
            return (flags >> 11) & 0x0F; // Extract OPCODE (bits 11-14)
        }
    }



    public record Question(String name, short type, short clazz) {}
}
