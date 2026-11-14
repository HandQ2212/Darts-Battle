package updater;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class UpdaterTest {
    public static void main(String[] args) throws Exception {
        KeyPair keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        byte[] payload = "version=2.5.1\nmac.sha256=abc\n".getBytes(StandardCharsets.US_ASCII);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate()); signer.update(payload);
        byte[] prefix = (Base64.getEncoder().encodeToString(signer.sign()) + "\n").getBytes(StandardCharsets.US_ASCII);
        byte[] signed = new byte[prefix.length + payload.length];
        System.arraycopy(prefix, 0, signed, 0, prefix.length);
        System.arraycopy(payload, 0, signed, prefix.length, payload.length);
        if (!Updater.verify(signed, keys.getPublic().getEncoded()).getProperty("version").equals("2.5.1")) throw new AssertionError();
        signed[signed.length - 2] ^= 1;
        try { Updater.verify(signed, keys.getPublic().getEncoded()); throw new AssertionError("tampered feed accepted"); }
        catch (java.io.IOException expected) { }
        if (Updater.compare("2.5.10", "2.5.9") <= 0 || Updater.compare("2.5", "2.5.0") != 0 || Updater.compare("2.4.9", "2.5") >= 0) throw new AssertionError("version comparison");
        try { Updater.compare("2.5-beta", "2.5"); throw new AssertionError("invalid version accepted"); }
        catch (java.io.IOException expected) { }
        System.out.println("Updater signature and version tests passed");
    }
}
