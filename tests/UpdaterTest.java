package updater;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

public class UpdaterTest {
    public static void main(String[] args) throws Exception {
        KeyPair keys = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        String windowsUrl = "https://github.com/HandQ2212/Darts-Battle/releases/latest/download/DartsBattle-windows.zip";
        byte[] payload = ("version=2.5.3\nwindows.url=" + windowsUrl + "\nwindows.sha256=" + "a".repeat(64) + "\n").getBytes(StandardCharsets.US_ASCII);
        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keys.getPrivate()); signer.update(payload);
        byte[] prefix = (Base64.getEncoder().encodeToString(signer.sign()) + "\n").getBytes(StandardCharsets.US_ASCII);
        byte[] signed = new byte[prefix.length + payload.length];
        System.arraycopy(prefix, 0, signed, 0, prefix.length);
        System.arraycopy(payload, 0, signed, prefix.length, payload.length);
        Properties release = Updater.verify(signed, keys.getPublic().getEncoded());
        if (!release.getProperty("version").equals("2.5.3") || !windowsUrl.equals(release.getProperty("windows.url"))
                || release.containsKey("mac.url")) throw new AssertionError("Windows-only manifest was not accepted");
        signed[signed.length - 2] ^= 1;
        try { Updater.verify(signed, keys.getPublic().getEncoded()); throw new AssertionError("tampered feed accepted"); }
        catch (java.io.IOException expected) { }
        if (Updater.compare("2.5.10", "2.5.9") <= 0 || Updater.compare("2.5", "2.5.0") != 0 || Updater.compare("2.4.9", "2.5") >= 0) throw new AssertionError("version comparison");
        try { Updater.compare("2.5-beta", "2.5"); throw new AssertionError("invalid version accepted"); }
        catch (java.io.IOException expected) { }
        System.out.println("Updater signature and version tests passed");
    }
}
