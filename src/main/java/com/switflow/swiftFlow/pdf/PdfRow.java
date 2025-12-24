package com.switflow.swiftFlow.pdf;

public class PdfRow {
    private String rowId;
    private int pageNumber;
    private float yPosition;
    private float pageHeight;
    private String text;

    public PdfRow(String rowId, int pageNumber, float yPosition, float pageHeight, String text) {
        this.rowId = rowId;
        this.pageNumber = pageNumber;
        this.yPosition = yPosition;
        this.pageHeight = pageHeight;
        this.text = text;
    }

    public String getRowId() {
        return rowId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public float getYPosition() {
        return yPosition;
    }

    public float getPageHeight() {
        return pageHeight;
    }

    public String getText() {
        return text;
    }
}
