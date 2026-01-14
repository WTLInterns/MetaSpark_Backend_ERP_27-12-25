package com.switflow.swiftFlow.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Response.PartsSelectionResponse;
import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PartsSelectionService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private StatusService statusService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public StatusResponse saveSelection(long orderId, Department roleDepartment, List<String> selectedRowIds) {
        if (roleDepartment == null) {
            throw new IllegalArgumentException("Unable to determine role");
        }

        List<String> selected = (selectedRowIds != null) ? selectedRowIds : List.of();

        try {
            String jsonComment = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "selectedRowIds", selected,
                            "partsSelection", true
                    )
            );

            StatusRequest request = new StatusRequest();
            request.setNewStatus(roleDepartment);
            request.setComment(jsonComment);
            request.setPercentage(null);
            request.setAttachmentUrl(null);

            return statusService.createCheckboxStatus(request, orderId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist parts selection", e);
        }
    }

    public PartsSelectionResponse getSelection(long orderId) {
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        if (statuses == null || statuses.isEmpty()) {
            return new PartsSelectionResponse(List.of(), List.of(), List.of(), List.of());
        }

        List<String> designer = extractLatestSelectedRowIds(statuses, Department.DESIGN);
        List<String> production = extractLatestSelectedRowIds(statuses, Department.PRODUCTION);
        List<String> machining = extractLatestSelectedRowIds(statuses, Department.MACHINING);
        List<String> inspection = extractLatestSelectedRowIds(statuses, Department.INSPECTION);

        return new PartsSelectionResponse(designer, production, machining, inspection);
    }

    private List<String> extractLatestSelectedRowIds(List<Status> statuses, Department department) {
        if (statuses == null || statuses.isEmpty() || department == null) {
            return List.of();
        }

        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == department
                        && s.getComment() != null
                        && s.getComment().contains("partsSelection")
                        && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            JsonNode arr = node.get("selectedRowIds");
            if (arr == null || !arr.isArray()) {
                return List.of();
            }
            List<String> ids = new ArrayList<>();
            for (JsonNode n : arr) {
                if (n != null && !n.isNull()) {
                    String v = n.asText();
                    if (v != null && !v.isBlank()) {
                        ids.add(v);
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }
}
