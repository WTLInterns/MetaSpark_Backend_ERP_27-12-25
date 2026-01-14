package com.switflow.swiftFlow.Response;

import java.util.List;

public class MaterialSelectionResponse {
    private List<String> designerSelectedRowIds;
    private List<String> productionSelectedRowIds;
    private List<String> machineSelectedRowIds;
    private List<String> inspectionSelectedRowIds;

    public MaterialSelectionResponse() {
    }

    public MaterialSelectionResponse(List<String> designerSelectedRowIds,
                                    List<String> productionSelectedRowIds,
                                    List<String> machineSelectedRowIds,
                                    List<String> inspectionSelectedRowIds) {
        this.designerSelectedRowIds = designerSelectedRowIds;
        this.productionSelectedRowIds = productionSelectedRowIds;
        this.machineSelectedRowIds = machineSelectedRowIds;
        this.inspectionSelectedRowIds = inspectionSelectedRowIds;
    }

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
