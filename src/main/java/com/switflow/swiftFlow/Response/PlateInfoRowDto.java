package com.switflow.swiftFlow.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PlateInfoRowDto {
    private int order;
    private String material;
    private String thickness;
    private String plateSize;
    private int partsCount;
    private String cutTotalLength;
    private String moveTotalLength;
    private String planProcessTime;
    private int count;
}
