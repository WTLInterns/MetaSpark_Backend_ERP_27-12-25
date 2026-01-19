package com.switflow.swiftFlow.Response;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class ResultBlockDto {
    private int resultNo;
    private String material;
    private String thickness;
    private String plateSize;
    private String planProcessTime;
    private String cutTotalLength;
    private String moveTotalLength;
    private String pierceCount;
    private List<ResultPartDto> parts; 
    private int partsCount;

public int getPartsCount() {
    return partsCount;
}

public void setPartsCount(int partsCount) {
    this.partsCount = partsCount;
}

}
