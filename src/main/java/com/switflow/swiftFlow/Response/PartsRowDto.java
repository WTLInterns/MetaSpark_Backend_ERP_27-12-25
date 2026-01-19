package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PartsRowDto {
    private String rowNo;
    private String partName;
    private String previewImageUrl;
    private String material;
    private String thickness;
    private Integer requiredQty;
    private Integer placedQty;
    private Double weightKg;
    private String timePerInstance;
    private Integer pierceQty;
    private Double cuttingLength;
    private boolean status;
    
    // New fields for PDF coordinate mapping
    private Integer pageNumber;
    private Float yPosition;
    
    // Size fields for nesting PDF support
    private String sizeX;
    private String sizeY;
}
