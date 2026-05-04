package de.graube.hkrservice.pdf;


import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.util.List;

/**
 * Erzeugt eine einfache Sammelanordnungs-PDF mit PDFBox.
 */
public class SammelanordnungPdfBox {

    /**
     * Generiert ein PDF fuer eine Transaktionssammlung.
     *
     * @param file Zieldatei
     * @param txs Transaktionsliste
     * @throws Exception bei I/O- oder PDF-Fehlern
     */
    public void generate(String file, List<?> txs) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);

        var cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 12);
        cs.newLineAtOffset(50, 700);
        cs.showText("Sammelanordnung - Anzahl: " + txs.size());
        cs.endText();

        cs.close();
        doc.save(new File(file));
        doc.close();
    }
}