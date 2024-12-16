import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DnsQuestion {

    public static byte[] question(String domain, int qtype, int qclass) throws IOException {
        final var domainBytes = DnsMessage.encodeDomain(domain);
        final var output = new ByteArrayOutputStream();
        output.write(domainBytes); // Encoded domain name
        output.write(new byte[]{
                (byte) ((qtype >> 8) & 0xFF), (byte) (qtype & 0xFF), // QTYPE
                (byte) ((qclass >> 8) & 0xFF), (byte) (qclass & 0xFF) // QCLASS
        });
        return output.toByteArray();
    }
}
