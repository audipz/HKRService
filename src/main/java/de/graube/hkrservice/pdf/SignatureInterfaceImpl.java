package de.graube.hkrservice.pdf;

import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;

/**
 * Minimalimplementierung von PDFBox SignatureInterface fuer RSA-Signaturen.
 */
public class SignatureInterfaceImpl implements SignatureInterface {

    private PrivateKey privateKey;
    private Certificate[] chain;

    /**
     * Erstellt eine neue Signaturkomponente.
     *
     * @param privateKey privater Schluessel
     * @param chain Zertifikatskette
     */
    public SignatureInterfaceImpl(PrivateKey privateKey, Certificate[] chain) {
        this.privateKey = privateKey;
        this.chain = chain;
    }

    /**
     * Signiert den vom PDF-Framework bereitgestellten Datenstrom.
     *
     * @param content zu signierender Inhalt
     * @return Signaturbytes
     * @throws IOException bei Signaturfehlern
     */
    @Override
    public byte[] sign(InputStream content) throws IOException {

        try {
            java.security.Signature signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);

            byte[] buffer = new byte[8192];
            int n;
            while ((n = content.read(buffer)) > 0) {
                signature.update(buffer, 0, n);
            }

            return signature.sign();

        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
