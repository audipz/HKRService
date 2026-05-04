package de.graube.hkrservice;

import java.math.BigDecimal;

public class Fixed200 {

    public static String fill(String val, int length) {
        if (val == null) val = "";
        return String.format("%-" + length + "s", val).substring(0, length);
    }

    public static String num(BigDecimal val, int length) {
        String s = val.movePointRight(2).toPlainString();
        return String.format("%" + length + "s", s).replace(' ', '0');
    }
}