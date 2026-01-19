package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaterialDataRowDto {
    private String sheetId;
    private String material;
    private String thickness;
    private String sizeX;
    private String sizeY;
    private Integer sheetQty;
    private String notes;
    private boolean status;
}
