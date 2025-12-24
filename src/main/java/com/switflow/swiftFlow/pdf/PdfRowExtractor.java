package com.switflow.swiftFlow.pdf;

import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PdfRowExtractor extends PDFTextStripper {

    private final List<PdfRow> rows = new ArrayList<>();

    public PdfRowExtractor() throws IOException {
        super();
        setSortByPosition(true);
    }

    public List<PdfRow> getRows() {
        return rows;
    }

    @Override
    protected void writePage() throws IOException {
        super.writePage();

        Map<Integer, Map<Float, List<TextPosition>>> pageLineMap = new TreeMap<>();
        // PDFTextStripper stores textPositions in charactersByArticle
        for (int page = getCurrentPageNo(); page <= getCurrentPageNo(); page++) {
            List<List<TextPosition>> charactersByArticle = getCharactersByArticle();
            float lineThreshold = 2.0f;

            Map<Float, List<TextPosition>> lineMap = new TreeMap<>();
            for (List<TextPosition> textPositions : charactersByArticle) {
                for (TextPosition text : textPositions) {
                    float y = text.getYDirAdj();
                    Float key = null;
                    for (Float existingY : lineMap.keySet()) {
                        if (Math.abs(existingY - y) <= lineThreshold) {
                            key = existingY;
                            break;
                        }
                    }
                    if (key == null) {
                        key = y;
                        lineMap.put(key, new ArrayList<>());
                    }
                    lineMap.get(key).add(text);
                }
            }
            pageLineMap.put(page, lineMap);

            int rowIndex = 0;
            // Use the page's height so the frontend can map PDF coordinates -> canvas coordinates accurately
            float pageHeight = getCurrentPage().getMediaBox().getHeight();
            for (Map.Entry<Float, List<TextPosition>> entry : lineMap.entrySet()) {
                Float y = entry.getKey();
                List<TextPosition> texts = entry.getValue();
                texts.sort(Comparator.comparing(TextPosition::getXDirAdj));
                StringBuilder sb = new StringBuilder();
                for (TextPosition t : texts) {
                    sb.append(t.getUnicode());
                }
                String text = sb.toString().trim();
                if (!text.isEmpty()) {
                    String rowId = "p" + page + "_r" + rowIndex;
                    rows.add(new PdfRow(rowId, page, y, pageHeight, text));
                    rowIndex++;
                }
            }
        }
    }
}
