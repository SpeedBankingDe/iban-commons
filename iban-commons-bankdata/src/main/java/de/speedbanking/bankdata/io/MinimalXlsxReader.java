/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.bankdata.io;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A deliberately minimal, dependency-free reader for the first worksheet of an XLSX (Office Open
 * XML spreadsheet) file, using only JDK-provided facilities: {@link ZipInputStream} to unpack the
 * XLSX ZIP container and {@code javax.xml.parsers} (DOM) to parse its two relevant XML parts.
 * <p>
 * This is <strong>not</strong> a general-purpose XLSX library. It deliberately supports only:
 * <ul>
 *   <li>the first worksheet ({@code xl/worksheets/sheet1.xml}), any further sheets are ignored,</li>
 *   <li>shared strings ({@code xl/sharedStrings.xml}), including simple rich-text runs,</li>
 *   <li>plain numeric/inline cell values (read back as their raw text content),</li>
 * </ul>
 * and explicitly does <strong>not</strong> support formulas, cell styles/number formatting, merged
 * cells, multiple sheets, or any other part of the OOXML spreadsheet format. It exists solely to
 * read the small, simple, single-sheet lookup tables this module needs (e.g. the Belgian NBB/BNB
 * full bank code list) without pulling in a full XLSX library such as Apache POI, keeping this
 * module free of runtime dependencies.
 *
 * @since 1.8.11
 */
public final class MinimalXlsxReader {

    private static final String SHARED_STRINGS_ENTRY = "xl/sharedStrings.xml";
    private static final String SHEET1_ENTRY         = "xl/worksheets/sheet1.xml";

    private MinimalXlsxReader() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Reads the first worksheet of an XLSX file into a list of rows, each row being an array of
     * cell values in column order (missing/empty trailing cells within a row are represented as
     * empty strings, not omitted).
     *
     * @param xlsxStream the raw XLSX bytes (a ZIP container), not closed by this method's caller
     *                    requirement, this method closes it
     * @return the worksheet rows, in row order; empty if the sheet has no rows
     * @throws IOException if the stream is not a readable ZIP/XLSX container, or the worksheet
     *                      part is missing or malformed
     */
    public static List<String[]> read(InputStream xlsxStream) throws IOException {
        byte[] sharedStringsXml = null;
        byte[] sheetXml = null;

        try (ZipInputStream zip = new ZipInputStream(xlsxStream)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (SHARED_STRINGS_ENTRY.equals(name)) {
                    sharedStringsXml = readAll(zip);
                } else if (SHEET1_ENTRY.equals(name)) {
                    sheetXml = readAll(zip);
                }
            }
        }

        if (sheetXml == null) {
            throw new IOException("Not a readable XLSX file, missing required part " + SHEET1_ENTRY);
        }

        List<String> sharedStrings = sharedStringsXml == null ? Collections.<String>emptyList() : parseSharedStrings(sharedStringsXml);
        return parseSheet(sheetXml, sharedStrings);
    }

    private static List<String> parseSharedStrings(byte[] xml) throws IOException {
        Element root = parseXml(xml).getDocumentElement();
        NodeList siNodes = root.getElementsByTagName("si");
        List<String> result = new ArrayList<>(siNodes.getLength());
        for (int i = 0; i < siNodes.getLength(); i++) {
            Element si = (Element) siNodes.item(i);
            // covers both a plain <si><t>text</t></si> entry and rich text runs <si><r><t>...</t></r>...</si>,
            // getElementsByTagName("t") finds <t> at either nesting depth and concatenates all runs
            NodeList textNodes = si.getElementsByTagName("t");
            StringBuilder text = new StringBuilder();
            for (int j = 0; j < textNodes.getLength(); j++) {
                text.append(textNodes.item(j).getTextContent());
            }
            result.add(text.toString());
        }
        return result;
    }

    private static List<String[]> parseSheet(byte[] xml, List<String> sharedStrings) throws IOException {
        Element root = parseXml(xml).getDocumentElement();
        NodeList rowNodes = root.getElementsByTagName("row");
        List<String[]> result = new ArrayList<>(rowNodes.getLength());
        for (int i = 0; i < rowNodes.getLength(); i++) {
            result.add(parseRow((Element) rowNodes.item(i), sharedStrings));
        }
        return result;
    }

    private static String[] parseRow(Element row, List<String> sharedStrings) {
        NodeList cellNodes = row.getElementsByTagName("c");
        String[] byColumn = new String[cellNodes.getLength()];
        int maxColumn = -1;
        for (int i = 0; i < cellNodes.getLength(); i++) {
            Element cell = (Element) cellNodes.item(i);
            int columnIndex = columnIndex(cell.getAttribute("r"));
            if (columnIndex < 0) {
                continue;
            }
            if (columnIndex >= byColumn.length) {
                byColumn = growTo(byColumn, columnIndex + 1);
            }
            byColumn[columnIndex] = cellValue(cell, sharedStrings);
            maxColumn = Math.max(maxColumn, columnIndex);
        }

        String[] result = new String[maxColumn + 1];
        for (int i = 0; i <= maxColumn; i++) {
            result[i] = byColumn[i] == null ? "" : byColumn[i];
        }
        return result;
    }

    private static String[] growTo(String[] original, int newLength) {
        String[] grown = new String[newLength];
        System.arraycopy(original, 0, grown, 0, original.length);
        return grown;
    }

    private static String cellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList textNodes = cell.getElementsByTagName("t");
            return textNodes.getLength() == 0 ? "" : textNodes.item(0).getTextContent();
        }

        NodeList valueNodes = cell.getElementsByTagName("v");
        String rawValue = valueNodes.getLength() == 0 ? "" : valueNodes.item(0).getTextContent();
        if ("s".equals(type)) {
            int sharedIndex = rawValue.isEmpty() ? -1 : Integer.parseInt(rawValue);
            return sharedIndex >= 0 && sharedIndex < sharedStrings.size() ? sharedStrings.get(sharedIndex) : "";
        }
        return rawValue;
    }

    /**
     * Converts a cell reference such as {@code "C5"} into a zero-based column index (2, here).
     *
     * @param cellRef the cell reference, may be empty
     * @return the zero-based column index, or -1 if {@code cellRef} has no leading column letters
     */
    private static int columnIndex(String cellRef) {
        int column = 0;
        int letterCount = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char c = cellRef.charAt(i);
            if (!Character.isLetter(c)) {
                break;
            }
            column = column * 26 + (Character.toUpperCase(c) - 'A' + 1);
            letterCount++;
        }
        return letterCount == 0 ? -1 : column - 1;
    }

    private static Document parseXml(byte[] xml) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // this module only ever parses XML parts it unpacked itself from a downloaded XLSX,
            // but disabling DOCTYPE/external entities is cheap, standard XXE hardening regardless
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException ex) {
            throw new IOException("Failed to parse XLSX XML part", ex);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toByteArray();
    }

}
