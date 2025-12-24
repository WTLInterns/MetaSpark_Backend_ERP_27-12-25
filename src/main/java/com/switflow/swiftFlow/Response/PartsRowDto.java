package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartsRowDto {
    private int rowNo;
    private String partName;
    private String previewImageUrl;
    private String material;
    private String thickness;
    private int requiredQty;
    private int placedQty;
    private double weightKg;
    private String timePerInstance;
    private int pierceQty;
    private double cuttingLength;
    private boolean status;
}
