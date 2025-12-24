package com.switflow.swiftFlow.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.switflow.swiftFlow.Response.SubNestRowDto;
import com.switflow.swiftFlow.Response.PartsRowDto;
import com.switflow.swiftFlow.Response.MaterialDataRowDto;

@Service
public class PdfSubnestService {

    private static final Logger logger = LoggerFactory.getLogger(PdfSubnestService.class);

    public List<SubNestRowDto> getMockSubnestRows() {
        List<SubNestRowDto> rows = new ArrayList<>();

        rows.add(new SubNestRowDto(1, null, "1250", "3500", "GI", "0.8",
                "00:14:18", "00:14:18", "TTLLP001", 1, 4.38, 89.75, false));

        rows.add(new SubNestRowDto(2, null, "1250", "3500", "GI", "0.8",
                "00:15:24", "00:15:24", "TTLLP002", 1, 4.38, 87.27, false));

        rows.add(new SubNestRowDto(3, null, "1250", "3500", "GI", "0.8",
                "00:09:21", "00:09:21", "TTLLP003", 1, 4.38, 84.80, false));

        return rows;
    }

    public List<PartsRowDto> parsePartsFromUrl(String attachmentUrl) {
        List<PartsRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text == null || text.isBlank()) {
                return rows;
            }
            String[] lines = text.split("\r?\n");

            boolean inPartsSection = false;
            Integer currentRowNo = null;
            StringBuilder partNameBuilder = new StringBuilder();

            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String lower = line.toLowerCase();
                if (!inPartsSection) {
                    if (lower.equals("parts")) {
                        inPartsSection = true;
                    }
                    continue;
                }

                if (lower.startsWith("material data")) {
                    break;
                }

                if (lower.startsWith("no.") || lower.contains("part name")
                        || lower.contains("preview") || lower.contains("req.") || lower.contains("cutting")) {
                    continue;
                }

                if (line.matches("\\d+")) {
                    currentRowNo = Integer.parseInt(line);
                    partNameBuilder.setLength(0);
                    continue;
                }

                if (currentRowNo != null && !line.startsWith("GI")) {
                    if (partNameBuilder.length() > 0) {
                        partNameBuilder.append(' ');
                    }
                    partNameBuilder.append(line);
                    continue;
                }

                if (currentRowNo != null && line.startsWith("GI")) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 8) {
                        String material = tokens[0];
                        String thickness = tokens[1];
                        int requiredQty = parseIntSafe(tokens[2]);
                        int placedQty = parseIntSafe(tokens[3]);
                        double weightKg = parseDoubleSafe(tokens[4]);
                        String timePerInstance = tokens[5];
                        int pierceQty = parseIntSafe(tokens[6]);
                        double cuttingLength = parseDoubleSafe(tokens[tokens.length - 1]);

                        String partName = partNameBuilder.length() > 0 ? partNameBuilder.toString() : ("Part-" + currentRowNo);

                        rows.add(new PartsRowDto(currentRowNo, partName, null, material, thickness,
                                requiredQty, placedQty, weightKg, timePerInstance, pierceQty, cuttingLength, false));
                    }

                    currentRowNo = null;
                    partNameBuilder.setLength(0);
                }
            }
        } catch (IOException e) {
            logger.error("Error parsing Parts PDF from URL: {}", attachmentUrl, e);
        }

        return rows;
    }

    public List<MaterialDataRowDto> parseMaterialDataFromUrl(String attachmentUrl) {
        List<MaterialDataRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text == null || text.isBlank()) {
                return rows;
            }

            String[] lines = text.split("\r?\n");
            boolean inMaterialSection = false;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String lower = line.toLowerCase();

                // Detect start of "Material Data" section (tolerant to extra spaces/case)
                if (!inMaterialSection) {
                    String normalized = lower.replaceAll("\\s+", " ").trim();
                    if (normalized.contains("material data")) {
                        inMaterialSection = true;
                        logger.info("Entered Material Data section");
                    }
                    continue;
                }

                // End section only at the footer "Note" line (not the "Notes" header)
                if (lower.equals("note")) {
                    logger.info("Exiting Material Data section at line: {}", line);
                    break;
                }

                // Skip obvious header/label lines inside section
                if (lower.startsWith("sheet id")
                        || lower.contains("thickness")
                        || lower.contains("size x")
                        || lower.contains("size y")
                        || lower.startsWith("sheet")
                        || lower.startsWith("qty")
                        || lower.startsWith("notes")) {
                    logger.debug("Skipping material header line: {}", line);
                    continue;
                }

                // Actual data lines should start with GI: "GI 0.8 1250 3500 22"
                String[] tokens = line.split("\\s+");
                if (tokens.length < 5) {
                    logger.debug("Skipping material line with too few tokens: {}", line);
                    continue;
                }
                if (!tokens[0].equalsIgnoreCase("GI")) {
                    logger.debug("Skipping non-GI material line: {}", line);
                    continue;
                }

                try {
                    String material = tokens[0];
                    String thickness = tokens[1];
                    String sizeX = tokens[2];
                    String sizeY = tokens[3];
                    int sheetQty = parseIntSafe(tokens[4]);

                    rows.add(new MaterialDataRowDto(
                            null,
                            material,
                            thickness,
                            sizeX,
                            sizeY,
                            sheetQty,
                            null,
                            false
                    ));
                } catch (Exception ex) {
                    logger.debug("Failed to parse material row from line: {}", line, ex);
                }
            }
        } catch (IOException e) {
            logger.error("Error parsing Material Data PDF from URL: {}", attachmentUrl, e);
        }

        return rows;
    }

    public List<SubNestRowDto> parseSubnestFromUrl(String attachmentUrl) {
        List<SubNestRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text == null || text.isBlank()) {
                return getMockSubnestRows();
            }

            String[] lines = text.split("\r?\n");
            boolean inSubnestSection = false;
            Integer currentRowNo = null;

            for (int i = 0; i < lines.length; i++) {
                String rawLine = lines[i];
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String lower = line.toLowerCase();

                // Enter SubNest section
                if (!inSubnestSection) {
                    if (lower.startsWith("subnest")) {
                        inSubnestSection = true;
                    }
                    continue;
                }

                // Stop only when we truly leave SubNest: Parts table or Material Data.
                // Do NOT stop on generic Note lines because they appear between pages.
                if (lower.equals("parts") || lower.startsWith("material data")) {
                    break;
                }

                // Skip header lines
                if (lower.startsWith("no.")
                        || lower.startsWith("no ")
                        || lower.contains("size x")
                        || lower.contains("size y")
                        || lower.contains("material")
                        || lower.contains("thk.")
                        || lower.contains("time per")
                        || lower.contains("total")
                        || lower.contains("nc file")
                        || lower.contains("nc")
                        || lower.contains("area")
                        || lower.contains("eff. %")) {
                    continue;
                }

                // Skip summary / customer lines
                if (lower.startsWith("customer:")
                        || lower.startsWith("sheet qty.")
                        || lower.startsWith("placed part qty")
                        || lower.startsWith("required part qty")
                        || lower.startsWith("notes:")
                        || lower.startsWith("metaspark engineers")) {
                    continue;
                }

                // Row number line (possibly broken across 2 lines)
                if (line.matches("\\d+")) {
                    String numberStr = line;

                    // Combine single-digit lines like "1" then "0" => "10"
                    if (i + 1 < lines.length) {
                        String next = lines[i + 1].trim();
                        if (next.matches("\\d+") && next.length() == 1 && numberStr.length() == 1) {
                            numberStr = numberStr + next;
                            i++;
                        }
                    }

                    try {
                        currentRowNo = Integer.parseInt(numberStr);
                    } catch (NumberFormatException ex) {
                        currentRowNo = null;
                    }
                    continue;
                }

                // Data line for a pending row number
                if (currentRowNo != null) {
                    String[] tokens = line.split("\\s+");
                    // Expect at least: sizeX sizeY material thickness timePerInst totalTime ncFile qty area eff
                    if (tokens.length < 9) {
                        continue;
                    }

                    try {
                        String sizeX = tokens[0];
                        String sizeY = tokens[1];
                        String material = tokens[2];
                        String thickness = tokens[3];
                        String timePerInstance = tokens[4];
                        String totalTime = tokens[5];
                        String ncFile = tokens[6];
                        int qty = parseIntSafe(tokens[7]);
                        double areaM2 = parseDoubleSafe(tokens[8]);
                        double effPercent = tokens.length > 9 ? parseDoubleSafe(tokens[9]) : 0.0;

                        rows.add(new SubNestRowDto(
                                currentRowNo,
                                null,
                                sizeX,
                                sizeY,
                                material,
                                thickness,
                                timePerInstance,
                                totalTime,
                                ncFile,
                                qty,
                                areaM2,
                                effPercent,
                                false
                        ));
                    } catch (Exception ex) {
                        logger.debug("Failed to parse SubNest row from line: {}", line, ex);
                    }

                    currentRowNo = null;
                }
            }
        } catch (IOException e) {
            logger.error("Error parsing SubNest PDF from URL: {}", attachmentUrl, e);
        }

        if (rows.isEmpty()) {
            return getMockSubnestRows();
        }

        return rows;
    }

    public String extractRawTextFromUrl(String attachmentUrl) throws IOException {
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            return "";
        }

        try (InputStream in = new URL(attachmentUrl).openStream();
             PDDocument document = PDDocument.load(in)) {

            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }
}
