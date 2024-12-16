import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class DnsMessage {

    private static final int OPCODE = 0b01111000; // Example OPCODE mask
    private static final int RD = 1 << 8; // Recursion Desired flag
    private static final int RCODE = 1 << 2; // Example RCODE mask

    public static byte[] header(byte[] received) throws IOException {
        // Read ID and second line of header
        final var id = ByteBuffer.wrap(received, 0, 2).order(BIG_ENDIAN).getShort();
        final var receivedSecondLine = ByteBuffer.wrap(received, 2, 2).order(BIG_ENDIAN).getShort();

        // Build the response header
        int secondLine = 1 << 15; // QR (Response)
        secondLine = setBits(secondLine, receivedSecondLine & OPCODE); // Copy OPCODE
        secondLine = setBits(secondLine, receivedSecondLine & RD); // Copy RD
        secondLine = setBits(secondLine, RCODE); // Set RCODE (example)

        // Return full header
        return ByteBuffer.allocate(12)
                .order(BIG_ENDIAN)
                .putShort(id) // ID
                .putShort((short) secondLine) // Flags
                .putShort((short) 1) // QDCOUNT (1 question)
                .putShort((short) 1) // ANCOUNT (1 answer)
                .putShort((short) 0) // NSCOUNT
                .putShort((short) 0) // ARCOUNT
                .array();
    }

    private static int setBits(int integer, int mask) {
        return integer | mask;
    }

    public static byte[] encodeDomain(String domain) throws IOException {
        final var output = new ByteArrayOutputStream();
        for (String part : domain.split("\\.")) {
            output.write(part.length());
            output.write(part.getBytes(UTF_8));
        }
        output.write(0); // Null byte terminator
        return output.toByteArray();
    }
}
