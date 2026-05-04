package de.graube.hkrservice.util;

import java.math.BigDecimal;

/**
 * Helfer fuer F15Z-Festlaengenfelder.
 */
public class Fixed200 {

    /**
     * Fuellt einen Textwert linksbuendig auf feste Laenge auf.
     */
    public static String fill(String val, int length) {
        if (val == null) val = "";
        return String.format("%-" + length + "s", val).substring(0, length);
    }

    /**
     * Formatiert numerische Werte als rechtsbuendiges, nullgefuelltes Feld in Cent.
     */
    public static String num(BigDecimal val, int length) {
        String s = val.movePointRight(2).toPlainString();
        return String.format("%" + length + "s", s).replace(' ', '0');
    }
}