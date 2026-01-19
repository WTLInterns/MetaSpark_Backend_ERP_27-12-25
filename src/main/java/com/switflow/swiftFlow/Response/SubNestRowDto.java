package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubNestRowDto {
    private String rowNo;
    private String preview;
    private String sizeX;
    private String sizeY;
    private String material;
    private String thickness;
    private String timePerInstance;
    private String totalTime;
    private String ncFile;
    private Integer qty;
    private Double areaM2;
    private Double efficiencyPercent;
    private boolean status;
    
    // New fields for PDF coordinate mapping
    private Integer pageNumber;
    private Float yPosition;
    private Float pageHeight;
}
