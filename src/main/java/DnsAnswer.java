import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DnsAnswer {

    public static byte[] answer(String domain, short type, short clazz, int ttl, byte[] ip) throws IOException {
        var output = new ByteArrayOutputStream();
        output.write(DnsMessage.encodeDomain(domain)); // Encoded domain name
        output.write(new byte[]{
                (byte) (type >> 8), (byte) (type & 0xFF), // TYPE
                (byte) (clazz >> 8), (byte) (clazz & 0xFF), // CLASS
                (byte) (ttl >> 24), (byte) (ttl >> 16), (byte) (ttl >> 8), (byte) ttl, // TTL
                (byte) 0x00, (byte) 0x04, // RDLENGTH
        });
        output.write(ip); // RDATA (IP address)
        return output.toByteArray();
    }
}
