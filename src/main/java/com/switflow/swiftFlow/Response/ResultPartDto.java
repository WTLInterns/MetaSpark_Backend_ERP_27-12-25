package com.switflow.swiftFlow.Response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResultPartDto {
    private String partName;
    private String size;
    private int count;
    private int nestCount;
    private int remainCount;
    private String processed;
}
