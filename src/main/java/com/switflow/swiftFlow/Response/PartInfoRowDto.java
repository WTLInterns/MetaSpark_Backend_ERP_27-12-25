package com.switflow.swiftFlow.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PartInfoRowDto {
    private int order;
    private String partName;
    private String size;
    private int partsCount;
    private int nestCount;
    private int remainCount;
    private String processed;
}
