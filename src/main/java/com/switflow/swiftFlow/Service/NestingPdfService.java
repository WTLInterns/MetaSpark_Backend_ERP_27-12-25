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

import com.switflow.swiftFlow.Response.PlateInfoRowDto;
import com.switflow.swiftFlow.Response.PartInfoRowDto;
import com.switflow.swiftFlow.Response.ResultBlockDto;
import com.switflow.swiftFlow.Response.ResultPartDto;

@Service
public class NestingPdfService {

    private static final Logger logger = LoggerFactory.getLogger(NestingPdfService.class);

    // =========================================================
    // ✅ URL normalize
    // =========================================================
    private String normalizeUrl(String url) {
        if (url == null) return "";
        url = url.trim();

        if (url.startsWith("://")) url = "https" + url;
        if (url.startsWith("http//")) url = url.replace("http//", "http://");
        if (url.startsWith("https//")) url = url.replace("https//", "https://");

        return url;
    }

    // =========================================================
    // ✅ Extract raw text from PDF URL
    // =========================================================
    private String extractRawTextFromUrl(String attachmentUrl) throws IOException {
        attachmentUrl = normalizeUrl(attachmentUrl);
        if (attachmentUrl == null || attachmentUrl.isBlank()) return "";

        try (InputStream in = new URL(attachmentUrl).openStream();
             PDDocument doc = PDDocument.load(in)) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setLineSeparator("\n");

            String raw = stripper.getText(doc);
            return raw == null ? "" : raw;
        }
    }

    // =========================================================
    // ✅ Extract section between headings
    // =========================================================
    private String extractSection(String fullText, String start, String end) {
        if (fullText == null) return "";
        Pattern p = Pattern.compile("(?is)" + Pattern.quote(start) + "(.*?)(?=" + Pattern.quote(end) + "|\\z)");
        Matcher m = p.matcher(fullText);
        return m.find() ? safe(m.group(1)) : "";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private int safeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    // =========================================================
    // ✅ TAB 1: Plate Info (ROW BASED ✅)
    // =========================================================
    public List<PlateInfoRowDto> parsePlateInfo(String attachmentUrl) {
        List<PlateInfoRowDto> rows = new ArrayList<>();

        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text.isBlank()) return rows;

            String plateSection = extractSection(text, "Plate Info", "Part Info");
            if (plateSection.isBlank()) return rows;

            // ✅ row format:
            // 1 1250.00 x 2500.00 2 0.00mm 0.00mm 0.4s 1
            Pattern rowPattern = Pattern.compile(
                    "(?m)^(\\d+)\\s+(\\d+\\.?\\d*)\\s*x\\s*(\\d+\\.?\\d*)\\s+(\\d+)\\s+([0-9.]+mm)\\s+([0-9.]+mm)\\s+([0-9.]+s)\\s+(\\d+)\\s*$"
            );

            Matcher matcher = rowPattern.matcher(plateSection);

            while (matcher.find()) {
                PlateInfoRowDto dto = new PlateInfoRowDto();
                dto.setOrder(safeInt(matcher.group(1)));
                dto.setPlateSize(matcher.group(2) + " x " + matcher.group(3));
                dto.setPartsCount(safeInt(matcher.group(4)));
                dto.setCutTotalLength(matcher.group(5));
                dto.setMoveTotalLength(matcher.group(6));
                dto.setPlanProcessTime(matcher.group(7));
                dto.setCount(safeInt(matcher.group(8)));

                dto.setMaterial("");
                dto.setThickness("");

                rows.add(dto);
            }

        } catch (Exception e) {
            logger.error("❌ Error parsing Plate Info PDF: {}", attachmentUrl, e);
        }

        return rows;
    }

    // =========================================================
    // ✅ TAB 2: Part Info (ROW BASED ✅ FIXED)
    // =========================================================
    public List<PartInfoRowDto> parsePartInfo(String attachmentUrl) {
        List<PartInfoRowDto> rows = new ArrayList<>();

        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text.isBlank()) return rows;

            String partSection = extractSection(text, "Part Info", "Nest Result");
            if (partSection.isBlank()) return rows;

            // ✅ row format:
            // 1 20251116A 1857.60 x 957.60 1 1 0 0
            Pattern rowPattern = Pattern.compile(
                    "(?m)^(\\d+)\\s+(\\d{8}[A-Za-z])\\s+(\\d+\\.?\\d*)\\s*x\\s*(\\d+\\.?\\d*)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*$"
            );

            Matcher matcher = rowPattern.matcher(partSection);

            while (matcher.find()) {
                PartInfoRowDto dto = new PartInfoRowDto();
                dto.setOrder(safeInt(matcher.group(1)));
                dto.setPartName(matcher.group(2));
                dto.setSize(matcher.group(3) + " x " + matcher.group(4));
                dto.setPartsCount(safeInt(matcher.group(5)));
                dto.setNestCount(safeInt(matcher.group(6)));
                dto.setRemainCount(safeInt(matcher.group(7)));
                dto.setProcessed(matcher.group(8));

                rows.add(dto);
            }

        } catch (Exception e) {
            logger.error("❌ Error parsing Part Info PDF: {}", attachmentUrl, e);
        }

        return rows;
    }

    // =========================================================
    // ✅ TAB 3: Results (Nest Result blocks + Parts List)
    // =========================================================
    public List<ResultBlockDto> parseResults(String attachmentUrl) {
        List<ResultBlockDto> results = new ArrayList<>();

        try {
            String text = extractRawTextFromUrl(attachmentUrl);
            if (text.isBlank()) return results;

            // ✅ Extract every Nest Result block
            Pattern resultPattern = Pattern.compile(
                    "(?is)Nest\\s*Result\\s*(\\d+)(.*?)(?=Nest\\s*Result\\s*\\d+|\\z)"
            );

            Matcher resultMatcher = resultPattern.matcher(text);

            while (resultMatcher.find()) {
                int resultNo = safeInt(resultMatcher.group(1));
                String block = safe(resultMatcher.group(2));

                ResultBlockDto dto = new ResultBlockDto();
                dto.setResultNo(resultNo);

                dto.setMaterial(extractInlineValue(block, "Material"));
                dto.setThickness(extractInlineValue(block, "Thickness"));

                dto.setPlateSize(extractInlinePlateSize(block));
                dto.setPlanProcessTime(extractInlineAfterLabel(block, "Plan Process Time"));
                dto.setCutTotalLength(extractInlineAfterLabel(block, "Cut Total Length"));
                dto.setMoveTotalLength(extractInlineAfterLabel(block, "Move Total Length"));
                dto.setPierceCount(extractInlineAfterLabel(block, "Pierce Count"));

                // ✅ Parts List section
                List<ResultPartDto> parts = extractResultParts(block);
                dto.setParts(parts);
                dto.setPartsCount(parts.size());

                results.add(dto);
            }

        } catch (Exception e) {
            logger.error("❌ Error parsing Results PDF: {}", attachmentUrl, e);
        }

        return results;
    }

    // =========================================================
    // ✅ Helpers for Results
    // =========================================================

    // Material: MS Thickness: 1.60mm
    private String extractInlineValue(String block, String label) {
        Matcher m = Pattern.compile("(?is)" + Pattern.quote(label) + "\\s*:\\s*([^\\n\\r]+)").matcher(block);
        if (m.find()) {
            String v = m.group(1).trim();

            if (label.equalsIgnoreCase("Material")) {
                if (v.contains("Thickness")) v = v.split("Thickness")[0];
            }
            return v.replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    // Plate Size: 1250.00 x 2500.00mm
    private String extractInlinePlateSize(String block) {
        Matcher m = Pattern.compile("(?is)Plate\\s*Size\\s*:\\s*(\\d+\\.?\\d*)\\s*x\\s*(\\d+\\.?\\d*)\\s*mm")
                .matcher(block);
        if (m.find()) {
            return m.group(1) + " x " + m.group(2);
        }
        return "";
    }

    // Cut Total Length: 0.00mm Plan Cut Time: 0s
    private String extractInlineAfterLabel(String block, String label) {
        Matcher m = Pattern.compile("(?is)" + Pattern.quote(label) + "\\s*:\\s*([^\\n\\r]+)")
                .matcher(block);
        if (m.find()) {
            String s = m.group(1).trim();
            // stop at "Plan"
            if (s.contains("Plan")) s = s.split("Plan")[0].trim();
            return s.replaceAll("\\s+", " ").trim();
        }
        return "";
    }

    // =========================================================
    // ✅ Extract parts list from result block
    // =========================================================
    private List<ResultPartDto> extractResultParts(String block) {
        List<ResultPartDto> list = new ArrayList<>();

        int idx = block.toLowerCase().indexOf("parts list");
        if (idx < 0) return list;

        String after = block.substring(idx);

        // ✅ Part row format:
        // 20251116E 1812.60 x 1047.60mm 1
        Pattern rowPattern = Pattern.compile(
                "(?m)^(\\d{8}[A-Za-z])\\s+(\\d+\\.?\\d*)\\s*x\\s*(\\d+\\.?\\d*)mm\\s+(\\d+)\\s*$"
        );

        Matcher m = rowPattern.matcher(after);
        while (m.find()) {
            ResultPartDto part = new ResultPartDto();
            part.setPartName(m.group(1));
            part.setSize(m.group(2) + " x " + m.group(3));
            part.setCount(safeInt(m.group(4)));
            list.add(part);
        }

        return list;
    }
}
