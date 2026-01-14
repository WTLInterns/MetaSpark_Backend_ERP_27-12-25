package com.switflow.swiftFlow.Request;

import java.util.List;

public class MaterialSelectionRequest {
    private List<String> designerSelectedRowIds;
    private List<String> productionSelectedRowIds;
    private List<String> machineSelectedRowIds;
    private List<String> inspectionSelectedRowIds;

    public List<String> getDesignerSelectedRowIds() {
        return designerSelectedRowIds;
    }

    public void setDesignerSelectedRowIds(List<String> designerSelectedRowIds) {
        this.designerSelectedRowIds = designerSelectedRowIds;
    }

    public List<String> getProductionSelectedRowIds() {
        return productionSelectedRowIds;
    }

    public void setProductionSelectedRowIds(List<String> productionSelectedRowIds) {
        this.productionSelectedRowIds = productionSelectedRowIds;
    }

    public List<String> getMachineSelectedRowIds() {
        return machineSelectedRowIds;
    }

    public void setMachineSelectedRowIds(List<String> machineSelectedRowIds) {
        this.machineSelectedRowIds = machineSelectedRowIds;
    }

    public List<String> getInspectionSelectedRowIds() {
        return inspectionSelectedRowIds;
    }

    public void setInspectionSelectedRowIds(List<String> inspectionSelectedRowIds) {
        this.inspectionSelectedRowIds = inspectionSelectedRowIds;
    }
}
