import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DnsAnswer {

    public static byte[] answer(String domain, int type, int clazz) throws IOException {
        final var domainBytes = DnsMessage.encodeDomain(domain);
        final var output = new ByteArrayOutputStream();
        output.write(domainBytes); // Encoded domain name
        output.write(new byte[]{
                (byte) ((type >> 8) & 0xFF), (byte) (type & 0xFF), // TYPE
                (byte) ((clazz >> 8) & 0xFF), (byte) (clazz & 0xFF), // CLASS
                0x00, 0x00, 0x00, 0x3C, // TTL (60 seconds)
                0x00, 0x04, // RDLENGTH (4 bytes for IPv4)
                (byte) 192, (byte) 168, (byte) 1, (byte) 1 // RDATA (IP Address)
        });
        return output.toByteArray();
    }
}
