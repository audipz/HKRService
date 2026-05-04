package de.graube.hkrservice.pdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.*;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.visible.*;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Calendar;

/**
 * Signiert PDF-Dokumente mit einem PKCS12-Zertifikat.
 */
public class PdfBoxSigner {

    private PrivateKey privateKey;
    private Certificate[] chain;

    /**
     * Laedt Schluesselmaterial aus einer PKCS12-Datei.
     *
     * @param p12Path Pfad zur Zertifikatsdatei
     * @param password Passwort
     * @throws Exception bei Lade- oder Kryptofehlern
     */
    public PdfBoxSigner(String p12Path, String password) throws Exception {

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new FileInputStream(p12Path), password.toCharArray());

        String alias = ks.aliases().nextElement();

        privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
        chain = ks.getCertificateChain(alias);
    }

    /**
     * Signiert ein bestehendes PDF und schreibt das Ergebnis in eine neue Datei.
     *
     * @param input Pfad zur Eingabedatei
     * @param output Pfad zur Ausgabedatei
     * @throws Exception bei I/O- oder Kryptofehlern
     */
    public void sign(String input, String output) throws Exception {

        PDDocument doc = PDDocument.load(new File(input));

        PDSignature signature = new PDSignature();
        signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
        signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

        signature.setName("F15Z System");
        signature.setLocation("DE");
        signature.setReason("Sammelanordnung");

        signature.setSignDate(Calendar.getInstance());

        doc.addSignature(signature, new SignatureInterfaceImpl(privateKey, chain));

        FileOutputStream fos = new FileOutputStream(output);
        doc.saveIncremental(fos);
        doc.close();
    }
}