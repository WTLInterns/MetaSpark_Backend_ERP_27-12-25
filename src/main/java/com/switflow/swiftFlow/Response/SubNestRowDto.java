package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubNestRowDto {
    private int rowNo;
    private String preview;
    private String sizeX;
    private String sizeY;
    private String material;
    private String thickness;
    private String timePerInstance;
    private String totalTime;
    private String ncFile;
    private int qty;
    private double areaM2;
    private double efficiencyPercent;
    private boolean status;
}
