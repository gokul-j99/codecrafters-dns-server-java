import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DnsQuestion {

    public static byte[] question(DnsMessage.Question question) throws IOException {
        var output = new ByteArrayOutputStream();
        output.write(DnsMessage.encodeDomain(question.name())); // Encoded domain name
        output.write(new byte[]{
                (byte) (question.type() >> 8), (byte) (question.type() & 0xFF), // QTYPE
                (byte) (question.clazz() >> 8), (byte) (question.clazz() & 0xFF)  // QCLASS
        });
        return output.toByteArray();
    }
}
