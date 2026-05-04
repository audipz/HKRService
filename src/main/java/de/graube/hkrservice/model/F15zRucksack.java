package de.graube.hkrservice.model;

/**
 * Einfacher Key-Value-Container fuer Parser/Gateway-Daten.
 */
public class F15zRucksack {
    public String key;
    public String[] values;

    /**
     * Erstellt einen Rucksackeintrag.
     *
     * @param key Schluessel
     * @param values Werte
     */
    public F15zRucksack(String key, String... values) {
        this.key = key;
        this.values = values;
    }
}