package com.switflow.swiftFlow.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class NestingSelectionResponse {
    private List<String> designerSelectedRowIds;
    private List<String> productionSelectedRowIds;
    private List<String> machiningSelectedRowIds;
    private List<String> inspectionSelectedRowIds;
}
