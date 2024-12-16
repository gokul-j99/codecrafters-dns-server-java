import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DnsMessage {

    private static final int RD = 1 << 8; // Recursion Desired
    private static final int RCODE = 0;  // No Error

    public static byte[] header(byte[] received) throws IOException {
        final var id = ByteBuffer.wrap(received, 0, 2).order(BIG_ENDIAN).getShort();
        final var receivedSecondLine = ByteBuffer.wrap(received, 2, 2).order(BIG_ENDIAN).getShort();
        int opcode = (receivedSecondLine >> 11) & 0x0F;

        int secondLine = 1 << 15; // QR (Response)
        secondLine |= opcode << 11; // Preserve Opcode
        secondLine |= receivedSecondLine & RD; // Copy RD
        secondLine |= RCODE; // Set RCODE

        return ByteBuffer.allocate(12)
                .order(BIG_ENDIAN)
                .putShort(id)
                .putShort((short) secondLine)
                .putShort((short) 1) // QDCOUNT (1 question)
                .putShort((short) 1) // ANCOUNT (1 answer)
                .putShort((short) 0) // NSCOUNT
                .putShort((short) 0) // ARCOUNT
                .array();
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

        var qtype = ByteBuffer.wrap(received, inputStream.available() - 4, 2).order(BIG_ENDIAN).getShort();
        var qclass = ByteBuffer.wrap(received, inputStream.available() - 2, 2).order(BIG_ENDIAN).getShort();

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
