package com.switflow.swiftFlow.Request;

import lombok.Data;
import java.util.List;

@Data
public class NestingSelectionRequest {
    private List<String> designerSelectedRowIds;
    private List<String> productionSelectedRowIds;
    private List<String> machineSelectedRowIds;
    private List<String> inspectionSelectedRowIds;
}
