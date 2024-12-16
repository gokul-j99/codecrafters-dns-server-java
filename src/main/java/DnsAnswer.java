import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DnsAnswer {

    public static byte[] answer(String domain, short type, short clazz, int ttl, byte[] ip) throws IOException {
        var output = new ByteArrayOutputStream();

        // Encode the domain name
        output.write(DnsMessage.encodeDomain(domain));

        // Add TYPE (2 bytes) and CLASS (2 bytes)
        output.write(new byte[]{
                (byte) (type >> 8), (byte) (type & 0xFF), // TYPE = 1 for A record
                (byte) (clazz >> 8), (byte) (clazz & 0xFF)  // CLASS = 1 for IN
        });

        // Add TTL (4 bytes)
        output.write(new byte[]{
                (byte) (ttl >> 24), (byte) (ttl >> 16), (byte) (ttl >> 8), (byte) ttl
        });

        // Add RDLENGTH (2 bytes) = Length of the IP address
        output.write(new byte[]{0x00, 0x04});

        // Add RDATA (IP address, 4 bytes)
        output.write(ip);

        return output.toByteArray();
    }
}
