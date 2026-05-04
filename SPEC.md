# Technische Spezifikation – Schnittstelle F15z

**Quelle:** VerfRiBeS-HKR, Anlage 2 – Satzbeschreibung der Anordnungsunterlagen zur Schnittstelle F15z, Version 3.5  
**Herausgeber:** Bundesministerium der Finanzen (BMF), Rundschreiben 07.12.2017  
**Dokumente:**
- [Anlage 2 (Satzbeschreibung F15z)](https://www.verwaltungsvorschriften-im-internet.de/pdf/BMF-IIA2-20171207-H-08-80-KF-001-A002.pdf)
- [Anlage 1 (Satzbeschreibung F13z)](https://www.verwaltungsvorschriften-im-internet.de/pdf/BMF-IIA2-20171207-H-08-80-KF-001-A001.pdf)
- [Verfahrensrichtlinie (HTML)](https://www.verwaltungsvorschriften-im-internet.de/bsvwvbund_07122017_IIA2H23000610002005.htm)

---

## 1. Allgemeines

Die F15z-Schnittstelle dient der elektronischen Übermittlung von Anordnungsdaten an das HKR-Verfahren des Bundes per SFTP oder ElsterFT.

- Dateiübertragung: **SFTP** oder **ElsterFT**
- Satzlänge: **570 Byte** (Kurz) oder **900 Byte** (Lang, z. B. bei BIC/IBAN in SK2)
- Steuerung über Feld 9 des Vorsatzes: `L` = 900 Byte, Blank = 570 Byte
- Fehlerhafter Datensatz führt zur **Ablehnung der gesamten Datei**
- Feldtypen: `A` = alphanumerisch, `N` = numerisch ungepackt
- Beträge: **immer in Eurocent** (keine Dezimalstelle), sofern nicht anders beschrieben
- Nicht belegte Felder: Leerzeichen (Typ A) oder Nullen (Typ N)

---

## 2. Dateistruktur

```
┌─────────────────────────────────┐
│  Satzkennung 1 – Dateivorsatz   │  (ein Satz pro Datei)
├─────────────────────────────────┤
│  Satzkennung 2 – Einzelzahlung  │  (je Anordnungsfall)
│  Satzkennung 3 – Festlegungen   │  (je nach Bedarf)
│  Satzkennung 7 – Umbuchungen    │  (je nach Bedarf)
│  Satzkennung 8 – Mitteldispo    │  (je nach Bedarf)
│  ...                            │
├─────────────────────────────────┤
│  Satzkennung 9 – Dateinachsatz  │  (ein Satz pro Datei)
└─────────────────────────────────┘
```

---

## 3. Satzkennung 1 – Dateivorsatz

| Nr.  | Feldbezeichnung              | Länge | Typ | Beschreibung / Pflicht                                            |
|------|------------------------------|-------|-----|-------------------------------------------------------------------|
| 1.1  | Satzkennung                  | 1     | A   | **`1`** (fest)                                                    |
| 1.2  | Version                      | 1     | A   | `0` oder Blank                                                    |
| 2    | Bereich (reserviert)         | 6     | A   | Nur in Absprache mit KKR belegen; sonst Blanks                    |
| 3    | Haushaltsjahr                | 4     | N   | Format `JJJJ`                                                     |
| 4    | Kennzeichen der Datei        | 8     | A   | Eindeutig je Bewirtschafter + Haushaltsjahr; linksbuendig, kein fuehrendes Leerzeichen |
| 5    | Bewirtschafternummer         | 8     | N   | Nummer des einreichenden Bewirtschafters                          |
| 6    | Filler                       | 7     | A   | Blanks                                                            |
| 7    | Datum der Sammelanordnung    | 6     | N   | Format `TTMMJJ`                                                   |
| 8    | Filler                       | 6     | A   | Blanks                                                            |
| 9    | Satzlänge                    | 1     | A   | `L` = 900 Byte, Blank = 570 Byte                                  |
| 10   | Kennzeichen Währung          | 1     | A   | `E` = Euro (Konstante)                                            |
| 11   | Funktion                     | 1     | A   | `L` = löschen, `N` oder Blank = neue Sammelanordnung              |
| 12   | Schlüsselart                 | 60    | A   | Blanks (z. Z.)                                                    |
| 13   | Kennung Software             | 20    | A   | Herstellerkennung des erzeugenden Systems                         |
| 14   | Einlieferernummer            | 8     | N   | Vom KKR vergeben; Nullen wenn nicht belegt                        |

**Mindestbreite des Vorsatzes: 134 Byte** (bis Feld 14 inkl.), aufgefüllt auf 570 oder 900 Byte.

---

## 4. Satzkennung 2 – Einzelzahlungsvorgänge

| Nr.  | Feldbezeichnung                       | Länge | Typ | Beschreibung                                                              |
|------|---------------------------------------|-------|-----|---------------------------------------------------------------------------|
| 1.1  | Satzkennung                           | 1     | A   | **`2`** (fest)                                                            |
| 1.2  | Version                               | 1     | A   | `0` oder Blank                                                            |
| 1.3  | Filler                                | 2     | A   | Blanks                                                                    |
| 2    | Dienststellenbezeichnung              | 25    | A   | Bezeichnung der anordnenden Dienststelle                                  |
| 3    | Belegnummer des Bewirtschafters       | 8     | N   | Format `TTMMJnnn`; nnn > 0; hochzählen je Datensatz                       |
| 4    | Verarbeitungsschlüssel (VSL)          | 5     | N   | 5-stelliger Buchungsfall-Code (s. Abschnitt 7)                            |
| 5    | Kennzeichen Art der Zahlung (AdZ)     | 1     | N   | 0=sonstige, 1=sonstige EZ, 2=Verrechnung EZ, 3–6=Sammelbeleg-Teile       |
| 6    | Bewirtschafternummer (TV)             | 8     | N   | Titelverwalternummer mit Prüfziffer (`03nnnnnn`)                          |
| 7    | Filler                                | 4     | N   | Nullen                                                                    |
| 8    | Haushaltsstelle / Titelkonto          | 10    | N   | > 0 (HHSt lt. Haushaltsplan + Prüfziffer)                                 |
| 9    | Objektnummer / Unterteil              | 10    | N   | > 0 wenn Konto weiter unterteilt; 0 sonst                                 |
| 10   | Satzart `101`                         | 3     | A   | Konstante `101`                                                           |
| 11   | Kassenzeichen                         | 12    | A   | Blanks oder Kassenzeichen                                                 |
| 12   | Kennzeichen Mahnverfahren             | 5     | N   | >= 0                                                                      |
| 13   | Satzart `H22`                         | 3     | A   | Konstante `H22`                                                           |
| 14   | Empfänger / Einzahler (1)             | 27    | A   | Klartextname des Empfängers (`> Blank`)                                   |
| 15   | Empfänger / Einzahler (2)             | 27    | A   | Fortsetzung des Namens                                                    |
| 16   | Straße / Postfach                     | 27    | A   | Pflicht bei Postbarzahlungen                                              |
| 17   | Filler                                | 3     | A   | Blanks                                                                    |
| 18   | PLZ (5) + Ort (22)                    | 27    | A   | Pflicht bei Postbarzahlungen                                              |
| 19   | Satzart `H01`                         | 3     | A   | Konstante `H01`                                                           |
| 20   | Filler (BLZ alt)                      | 8     | N   | Nullen (BLZ entfallen)                                                    |
| 21   | Filler (KtoNr alt)                    | 10    | N   | Nullen (Kontonummer entfallen)                                            |
| 22   | Filler (Institut)                     | 27    | A   | Blanks                                                                    |
| 23   | Satzart `100`                         | 3     | A   | Konstante `100`                                                           |
| 24   | Betrag                                | 13    | N   | **In Eurocent** (= Betrag × 100); 0 nur bei Devisenauslandszahlun. mit festem FX |
| 25   | Fälligkeit / Bezugsdatum              | 6     | N   | Format `TTMMJJ`; 0 = sofortige Fälligkeit                                 |
| 26   | Kennzeichen Gutschrift auf EK-Konto   | 1     | N   | 1 = Gutschrift zum Fälligkeitsdatum, 0 = sonstige                        |
| 27   | Bezugsbelegnummer                     | 8     | N   | > 0 bei Aufhebung einer terminierten Anordnung; 0 sonst                  |
| 28   | Satzart `H32`                         | 3     | A   | Konstante `H32`                                                           |
| 29   | Verwendungszweck Überweisungsträger   | 27    | A   | > Blank bei unbarer Zahlung                                               |
| 30   | Satzart `H02`                         | 3     | A   | Konstante `H02`                                                           |
| 31   | Buchungstext / Abschlagsdaten         | 25    | A   | Wahlweise                                                                 |
| 32   | Satzart `H12`                         | 3     | A   | Konstante `H12`                                                           |
| 33   | Buchungstext (2)                      | 25    | A   | Wahlweise                                                                 |
| 34   | Satzart `104`                         | 3     | A   | Konstante `104`                                                           |
| 35   | Kennung E08                           | 1     | A   | `E` = E08-Anordnung beigefügt; Blank sonst                                |
| 35a  | Filler                                | 15    | N   | Nullen                                                                    |
| 36   | Von Festlegungsmitteln abzubuchender Betrag | 10 | N | 0 wenn nicht auf Festlegung Bezug genommen                              |
| 37   | Satzart `H82`                         | 3     | A   | Konstante `H82`                                                           |
| 38   | Mehrzweckfeld                         | 15    | A   | Blanks                                                                    |
| 39   | Satzart `E55`                         | 3     | A   | Konstante `E55`                                                           |
| 40–44| Begründung / Verweis begründ. Unterlagen | 5×27 | A | Wahlweise                                                              |
| 45   | Kennzeichen Zahlungsweg               | 1     | A   | `T`=eilbedürftig, `B`=reserviert, Blank=sonstige                         |
| 46   | Kennung Geschäftsvorfall              | 1     | A   | `A`=AWV-Meldung, `B`=Buchausgleich, Blank=sonstige                       |
| 47   | Empfangender Bewirtschafter (Buchausgl.) | 8  | A   | TV-Nummer; Blanks sonst                                                   |
| 48   | Kennung BIC                           | 3     | A   | Konstante `BIC` (bei unbaren Euro-Auszahlung.)                            |
| 49   | BIC                                   | 11    | A   | Bank Identifier Code des empfangenden Kreditinstituts                    |
| 50   | Kennung IBAN                          | 4     | A   | Konstante `IBAN` (bei unbaren Euro-Auszahlung.)                           |
| 51   | IBAN                                  | 34    | A   | International Bank Account Number des Zahlungsempfängers                 |

**Gesamtbreite SK2 Kurzformat:** aufgefüllt auf **570 Byte**; Langformat **900 Byte**.

---

## 5. Satzkennung 9 – Dateinachsatz (Trailer)

| Nr.  | Feldbezeichnung              | Länge | Typ | Beschreibung                                                   |
|------|------------------------------|-------|-----|----------------------------------------------------------------|
| 1.1  | Satzkennung                  | 1     | A   | **`9`** (fest)                                                 |
| 1.2  | Version                      | 1     | A   | `0` oder Blank                                                 |
| 2    | Filler                       | 6     | A   | Blanks                                                         |
| 3    | Haushaltsjahr                | 4     | N   | wie Satzkennung 1                                              |
| 4    | Kennzeichen der Datei        | 8     | A   | wie Satzkennung 1                                              |
| 5    | Bewirtschafternummer         | 8     | N   | wie Satzkennung 1                                              |
| 6    | Gesamtsumme                  | **14**| N   | Summe aller Beträge in Eurocent (inkl. Eurocent-Anteil)        |
| 7    | Anzahl Datensätze            | **5** | N   | **Einschließlich Vor- und Nachsatz**                           |
| 8    | Filler (BLZ-Summe alt)       | 15    | N   | Nullen                                                         |
| 9    | Filler (Kto-Summe alt)       | 15    | N   | Nullen                                                         |
| 10   | Authentifikator              | 16    | A   | Blanks (z. Z.)                                                 |
| 11   | Prüfsumme BIC                | 20    | N   | Numerische Summe der enthaltenen BIC-Werte                     |
| 12   | Prüfsumme IBAN               | 20    | N   | Numerische Summe der enthaltenen IBAN-Werte                    |

**Breite Nachsatz:** aufgefüllt auf 570 Byte.

---

## 6. Verarbeitungsschlüssel (VSL) – relevante Werte

| VSL-Code | Buchungsfall                                               | Enum-Mapping |
|----------|------------------------------------------------------------|--------------|
| `52000`  | Einmalige unbare Euro-Auszahlung (ohne Festlegung)         | `AUSZ`       |
| `52010`  | Abschlagsauszahlung (ohne Festlegung)                      | `AUSZ`       |
| `52020`  | Schlussauszahlung (ohne Festlegung)                        | `AUSZ`       |
| `53000`  | Erstattungen (Einzahlung ohne Verfügbarkeitserhöhung)      | `EINZ`       |
| `53100`  | Beiträge Dritter (Einzahlung)                              | `EINZ`       |
| `53200`  | Rückeinnahmen (Einzahlung)                                 | `EINZ`       |
| `68500`  | Umbuchung einer Auszahlung                                 | `UMB`        |
| `68510`  | Umbuchung einer Einzahlung                                 | `UMB`        |
| `41000`  | Festlegung von Haushaltsmitteln                            | `RES`        |
| `40500`  | Aufhebung einer Festlegung                                 | `RES`        |

---

## 7. Belegnummer-Format

- **Format:** `TTMMJnnn`
- **TTMMJ:** Tag (2), Monat (2), letzte Stelle Jahreszahl (1)
- **nnn:** laufende Nummer (3-stellig, mit führenden Nullen, `nnn > 0`)
- **Beispiel:** `04051001` = 04. Mai 2021, laufende Nummer 001
- Alternativ: haushaltsjahreseindeutige anderweitige Nummerierung in Absprache mit dem KKR
- **Länge:** 8 Byte numerisch

---

## 8. Betragskodierung

- Beträge werden **in Eurocent** angegeben (ganzzahlig, ohne Dezimaltrennzeichen)
- `num()`-Hilffunktion: `Betrag × 100 → ganzzahlig → rechtsbündig, null-gefüllt`
- Beispiel: `123.45 € → "0000000012345"` (13-stellig)

---

## 9. Datum-Format

- **Vorsatz (Feld 7):** `TTMMJJ` (6 Stellen numerisch)
- **SK2 Feld 25 (Fälligkeit):** `TTMMJJ` (6 Stellen numerisch); `0` = sofortige Fälligkeit

---

## 10. Validierungsregeln (Trailer-Check)

Der Dateinachsatz (SK9) muss folgende Bedingungen erfüllen:

| Feld     | Prüfregel                                                             |
|----------|-----------------------------------------------------------------------|
| Feld 6   | Gesamtsumme == Summe aller Beträge aus SK2 (in Eurocent)              |
| Feld 7   | Anzahl == tatsächliche Anzahl aller Datensätze **inklusive SK1 + SK9** |
| Feld 11  | Prüfsumme BIC == numerische Summe aller BIC-Werte gemäß Berechnungsregel |
| Feld 12  | Prüfsumme IBAN == numerische Summe aller IBAN-Werte gemäß Berechnungsregel |

---

## 11. Datei-Integrität

- Eine einmal unter einem Namen übermittelte Datei **darf nicht erneut** unter gleichem Namen eingereicht werden (Eindeutigkeitsprüfung des Dateinamens).
- Fehlerhafte Dateien können **ohne Löschung** erneut übertragen werden.
- Löschen ist erst nach erfolgreicher Übertragung erforderlich.
- Dubletten-Prüfung auf Belegnummer innerhalb eines Jobs.

---

## 12. Zurückgemeldete Dateien (Rückmeldeformat)

Rückmeldedateien nutzen ein **Pipe-separiertes Format** (`|`):

```
<SK>|<belegnummer>|<status>[|<message>]
```

- Präfix `2|` = Rückmeldung für einen Einzelzahlungsvorgang
- Präfix `R|` = generische Rückmeldung
- Status-Werte: `OK`, `SUCCESS`, `CONFIRMED` = erfolgreich; alle anderen = fehlerhaft
- Eine Rückmeldung enthält optional eine Nachricht als 4. Feld

Trailer-Rückmeldung:
```
9|<anzahl>|<summe>
```

---

## 13. Umsetzungsstatus (Stand: 2026-05-04)

Die Kernabweichungen wurden in der Implementierung (`F15zGenerator`, `F15zFileParser`, `F15zValidator`) behoben und per Tests abgesichert.

| Bereich | Status | Hinweis |
|---|---|---|
| Satzlänge SK1/SK2/SK9 | ✅ umgesetzt | 570 Byte pro Satz |
| VSL-Kodierung | ✅ umgesetzt | 5-stellige Codes (`52000`, `53100`, `68500`, `41000`) |
| SK2-Kernfeldlayout | ✅ umgesetzt | feste Positionen inkl. Betrag Feld 24 (13 N) |
| SK9 Feld 6 (Summe) | ✅ umgesetzt | 14 N, Eurocent |
| SK9 Feld 7 (Anzahl) | ✅ umgesetzt | 5 N, inkl. SK1 + SK9 |
| Validator-Countlogik | ✅ umgesetzt | unterscheidet Trailer mit/ohne Envelope-Zaehlung |
| Pipe-Rueckmeldedateien | ✅ beibehalten | kompatibel zu `2|...` / `R|...` / `9|...` |

### Offene Restpunkte

- SK9-Pruefsummen fuer BIC/IBAN (Felder 11/12) werden aktuell als `0...0` gefuellt.
- Der implementierte Generator deckt den in diesem Service verwendeten Teilumfang ab (insbesondere SK2-Auszahlungen), nicht den kompletten F15z-Funktionsumfang aller Satzkennungen.

