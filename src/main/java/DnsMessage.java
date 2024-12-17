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

    public static byte[] header(byte[] received) throws IOException {
        // Extract ID and flags using DataInputStream
        try (var inputStream = new ByteArrayInputStream(received);
             var dataInputStream = new DataInputStream(inputStream)) {

            // Read ID and Flags
            final short id = dataInputStream.readShort();
            final short receivedFlags = dataInputStream.readShort();

            // Extract Opcode from received flags
            int opcode = (receivedFlags >> 11) & 0x0F;

            // Build new flags for the response
            int responseFlags = 1 << 15; // QR = 1 (Response)
            responseFlags |= opcode << 11; // Preserve Opcode
            responseFlags |= receivedFlags & RD; // Preserve RD
            responseFlags |= RCODE; // Set RCODE

            // Build and return the header
            return ByteBuffer.allocate(12)
                    .order(BIG_ENDIAN)
                    .putShort(id) // ID
                    .putShort((short) responseFlags) // Flags
                    .putShort((short) 1) // QDCOUNT (1 question)
                    .putShort((short) 1) // ANCOUNT (1 answer)
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

    public record Question(String name, short type, short clazz) {}
}
