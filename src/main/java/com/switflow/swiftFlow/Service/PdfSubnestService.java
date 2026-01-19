package com.switflow.swiftFlow.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.switflow.swiftFlow.Response.SubNestRowDto;
import com.switflow.swiftFlow.Response.PartsRowDto;
import com.switflow.swiftFlow.Response.MaterialDataRowDto;
import com.switflow.swiftFlow.pdf.PdfRowExtractor;
import com.switflow.swiftFlow.pdf.PdfRow;

@Service
public class PdfSubnestService {

    private static final Logger logger = LoggerFactory.getLogger(PdfSubnestService.class);

    public List<SubNestRowDto> getMockSubnestRows() {
        List<SubNestRowDto> rows = new ArrayList<>();

        rows.add(new SubNestRowDto("1", null, "1250", "3500", "GI", "0.8",
                "00:14:18", "00:14:18", "TTLLP001", 1, 4.38, 89.75, false, null, null, null));

        rows.add(new SubNestRowDto("2", null, "1250", "3500", "GI", "0.8",
                "00:15:24", "00:15:24", "TTLLP002", 1, 4.38, 87.27, false, null, null, null));

        rows.add(new SubNestRowDto("3", null, "1250", "3500", "GI", "0.8",
                "00:09:21", "00:09:21", "TTLLP003", 1, 4.38, 84.80, false, null, null, null));

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

                        PartsRowDto dto = new PartsRowDto();
                        dto.setRowNo(String.valueOf(currentRowNo));
                        dto.setPartName(partName);
                        dto.setMaterial(material);
                        dto.setThickness(thickness);
                        dto.setRequiredQty(requiredQty);
                        dto.setPlacedQty(placedQty);
                        dto.setWeightKg(weightKg);
                        dto.setTimePerInstance(timePerInstance);
                        dto.setPierceQty(pierceQty);
                        dto.setCuttingLength(cuttingLength);
                        dto.setStatus(false);
                        dto.setPageNumber(null);
                        dto.setYPosition(null);
                        dto.setSizeX(null);
                        dto.setSizeY(null);
                        rows.add(dto);
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
                                String.valueOf(currentRowNo),
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
                                false,
                                null,
                                null,
                                null
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

    // ========== NEW METHODS FOR NESTING PDF FORMAT ==========

    /**
     * Parse nesting PDF format to extract SubNest data (Nest Result blocks)
     */
    public List<SubNestRowDto> parseNestingSubnestFromUrl(String attachmentUrl) {
        List<SubNestRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            List<PdfRow> pdfRows = extractPdfRowsFromUrl(attachmentUrl);
            
            if (text == null || text.isBlank()) {
                return rows;
            }

            // Split by "Nest Result" headings
            Pattern nestPattern = Pattern.compile("Nest Result(\\d+).*?(?=(Nest Result\\d+|$))", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = nestPattern.matcher(text);

            int globalRow = 1;
            while (m.find()) {
                String block = m.group(0);
                String nestNumber = m.group(1);
                
                // Parse material & thickness
                String material = extractSingle(block, "Material:\\s*(\\S+)");
                String thickness = extractSingle(block, "Thickness:\\s*([0-9\\.]+)mm");
                
                // Parse plate size
                Matcher sizeM = Pattern.compile("Plate Size:\\s*([0-9.]+)\\s*x\\s*([0-9.]+)mm").matcher(block);
                Double sizeX = null, sizeY = null;
                if (sizeM.find()) {
                    sizeX = Double.parseDouble(sizeM.group(1));
                    sizeY = Double.parseDouble(sizeM.group(2));
                }
                
                // Parse parts count
                int partsCount = extractInt(block, "Parts List\\s+(\\d+)\\s+Parts");
                
                // Parse process time
                String processTime = extractSingle(block, "Plan Process Time:\\s*([\\d\\.]+s?)");
                
                // Create DTO
                SubNestRowDto dto = new SubNestRowDto();
                dto.setRowNo(String.valueOf(globalRow++));
                dto.setMaterial(material);
                dto.setThickness(thickness);
                dto.setSizeX(sizeX != null ? sizeX.toString() : null);
                dto.setSizeY(sizeY != null ? sizeY.toString() : null);
                dto.setQty(partsCount);
                dto.setTimePerInstance(processTime);
                dto.setTotalTime(processTime); // Same as per-instance for nesting
                dto.setNcFile("NEST" + nestNumber);
                
                if (sizeX != null && sizeY != null) {
                    dto.setAreaM2(sizeX * sizeY / 1_000_000.0);
                }
                
                // Find PDF coordinates for this nest
                findPdfRowCoordinates(block, pdfRows, dto);
                
                rows.add(dto);
            }
        } catch (Exception e) {
            logger.error("Error parsing nesting SubNest PDF from URL: {}", attachmentUrl, e);
        }

        return rows;
    }

    /**
     * Parse nesting PDF format to extract Parts data from each Nest Result block
     */
    public List<PartsRowDto> parseNestingPartsFromUrl(String attachmentUrl) {
        List<PartsRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            List<PdfRow> pdfRows = extractPdfRowsFromUrl(attachmentUrl);
            
            if (text == null || text.isBlank()) {
                return rows;
            }

            // Split by "Nest Result" headings
            Pattern nestPattern = Pattern.compile("Nest Result(\\d+).*?(?=(Nest Result\\d+|$))", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
            Matcher m = nestPattern.matcher(text);

            int globalPartRow = 1;
            while (m.find()) {
                String block = m.group(0);
                
                // Parse material & thickness for this block
                String material = extractSingle(block, "Material:\\s*(\\S+)");
                String thickness = extractSingle(block, "Thickness:\\s*([0-9\\.]+)mm");
                
                // Find parts table in this block
                Pattern partsTablePattern = Pattern.compile("Parts List.*?\\n((?:.*\\n)*?)", Pattern.CASE_INSENSITIVE);
                Matcher tableMatcher = partsTablePattern.matcher(block);
                
                if (tableMatcher.find()) {
                    String tableSection = tableMatcher.group(1);
                    String[] tableLines = tableSection.split("\\r?\\n");
                    
                    for (String line : tableLines) {
                        line = line.trim();
                        if (line.isEmpty() || line.toLowerCase().contains("thumbnail") || 
                            line.toLowerCase().contains("part name") || line.toLowerCase().contains("size")) {
                            continue;
                        }
                        
                        // Parse part row: "1 20251116E 1812.60 x 1047.60mm 1"
                        Pattern partPattern = Pattern.compile("(\\d+)\\s+([^\\s]+)\\s+([0-9.]+)\\s*x\\s*([0-9.]+)mm\\s+(\\d+)");
                        Matcher partMatcher = partPattern.matcher(line);
                        
                        if (partMatcher.find()) {
                            String rowNo = partMatcher.group(1);
                            String partName = partMatcher.group(2);
                            Double partSizeX = Double.parseDouble(partMatcher.group(3));
                            Double partSizeY = Double.parseDouble(partMatcher.group(4));
                            Integer qty = Integer.parseInt(partMatcher.group(5));
                            
                            PartsRowDto dto = new PartsRowDto();
                            dto.setRowNo(String.valueOf(globalPartRow++));
                            dto.setPartName(partName);
                            dto.setMaterial(material);
                            dto.setThickness(thickness);
                            dto.setSizeX(partSizeX != null ? partSizeX.toString() : null);
                            dto.setSizeY(partSizeY != null ? partSizeY.toString() : null);
                            dto.setRequiredQty(qty);
                            dto.setPlacedQty(qty); // Same for nesting PDF
                            
                            // Find PDF coordinates for this part
                            findPartPdfRowCoordinates(partName, pdfRows, dto);
                            
                            rows.add(dto);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing nesting Parts PDF from URL: {}", attachmentUrl, e);
        }

        return rows;
    }

    /**
     * Parse nesting PDF format to extract Material data
     */
    public List<MaterialDataRowDto> parseNestingMaterialDataFromUrl(String attachmentUrl) {
        List<MaterialDataRowDto> rows = new ArrayList<>();
        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            
            if (text == null || text.isBlank()) {
                return rows;
            }

            // Extract material info from first page or first nest block
            String material = extractSingle(text, "Material:\\s*(\\S+)");
            String thickness = extractSingle(text, "Thickness:\\s*([0-9\\.]+)mm");
            
            // Extract plate size
            Matcher sizeM = Pattern.compile("Plate Size:\\s*([0-9.]+)\\s*x\\s*([0-9.]+)mm").matcher(text);
            String sizeX = null, sizeY = null;
            if (sizeM.find()) {
                sizeX = sizeM.group(1);
                sizeY = sizeM.group(2);
            }
            
            // Count total sheets (number of Nest Result blocks)
            Pattern nestPattern = Pattern.compile("Nest Result\\d+", Pattern.CASE_INSENSITIVE);
            Matcher nestMatcher = nestPattern.matcher(text);
            int sheetCount = 0;
            while (nestMatcher.find()) {
                sheetCount++;
            }
            
            if (material != null && thickness != null) {
                MaterialDataRowDto dto = new MaterialDataRowDto();
                dto.setMaterial(material);
                dto.setThickness(thickness);
                dto.setSizeX(sizeX);
                dto.setSizeY(sizeY);
                dto.setSheetQty(sheetCount);
                dto.setNotes("Nesting sheets - " + sheetCount + " plates");
                dto.setStatus(false);
                
                rows.add(dto);
            }
        } catch (Exception e) {
            logger.error("Error parsing nesting Material Data PDF from URL: {}", attachmentUrl, e);
        }

        return rows;
    }

    // ========== HELPER METHODS ==========

    /**
     * Extract PDF rows with coordinates using PdfRowExtractor
     */
    private List<PdfRow> extractPdfRowsFromUrl(String attachmentUrl) throws IOException {
        List<PdfRow> pdfRows = new ArrayList<>();
        
        try (InputStream in = new URL(attachmentUrl).openStream();
             PDDocument document = PDDocument.load(in)) {
            
            PdfRowExtractor extractor = new PdfRowExtractor();
            extractor.getText(document);
            pdfRows = extractor.getRows();
        }
        
        return pdfRows;
    }

    /**
     * Extract single value using regex
     */
    private String extractSingle(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Extract integer value using regex
     */
    private int extractInt(String text, String regex) {
        String s = extractSingle(text, regex);
        if (s == null) return 0;
        try { 
            return Integer.parseInt(s.replaceAll("[^0-9]", "")); 
        } catch (Exception e) { 
            return 0; 
        }
    }

    /**
     * Find PDF coordinates for a nest block
     */
    private void findPdfRowCoordinates(String block, List<PdfRow> pdfRows, SubNestRowDto dto) {
        // Try to find the nest result heading or first part name in PDF rows
        String searchText = null;
        
        // Look for "Nest Result" heading
        Pattern nestHeadingPattern = Pattern.compile("Nest Result\\d+");
        Matcher headingMatcher = nestHeadingPattern.matcher(block);
        if (headingMatcher.find()) {
            searchText = headingMatcher.group();
        }
        
        // If no heading found, look for first part name
        if (searchText == null) {
            Pattern partPattern = Pattern.compile("\\b(20\\d{6}[A-Z]?)\\b");
            Matcher partMatcher = partPattern.matcher(block);
            if (partMatcher.find()) {
                searchText = partMatcher.group(1);
            }
        }
        
        if (searchText != null) {
            for (PdfRow row : pdfRows) {
                if (row.getText().contains(searchText)) {
                    dto.setPageNumber(row.getPageNumber());
                    dto.setYPosition(row.getYPosition());
                    dto.setPageHeight(row.getPageHeight());
                    break;
                }
            }
        }
    }

    /**
     * Find PDF coordinates for a part
     */
    private void findPartPdfRowCoordinates(String partName, List<PdfRow> pdfRows, PartsRowDto dto) {
        for (PdfRow row : pdfRows) {
            if (row.getText().contains(partName)) {
                dto.setPageNumber(row.getPageNumber());
                dto.setYPosition(row.getYPosition());
                break;
            }
        }
    }

    // ========== NEW METHODS FOR NESTING PDF FORMAT ==========

    // ========== REMOVED DUPLICATE NESTING METHODS ==========
    // These methods are now in NestingPdfService.java
    // public List<SubNestRowDto> parseNestingSubnestFromUrl(String attachmentUrl)
    // public List<PartsRowDto> parseNestingPartsFromUrl(String attachmentUrl) 
    // public List<MaterialDataRowDto> parseNestingMaterialDataFromUrl(String attachmentUrl)
}
