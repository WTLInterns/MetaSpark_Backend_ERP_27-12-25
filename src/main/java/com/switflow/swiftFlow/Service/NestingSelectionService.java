package com.switflow.swiftFlow.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Response.NestingSelectionResponse;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class NestingSelectionService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private StatusService statusService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void saveSelection(long orderId, Department department, List<String> selectedRowIds, String flagKey) {
        try {
            String jsonComment = objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "selectedRowIds", selectedRowIds != null ? selectedRowIds : List.of(),
                            flagKey, true
                    )
            );

            StatusRequest request = new StatusRequest();
            request.setNewStatus(department);
            request.setComment(jsonComment);
            request.setPercentage(null);
            request.setAttachmentUrl(null);

            statusService.createCheckboxStatus(request, orderId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save nesting selection", e);
        }
    }

    public NestingSelectionResponse getSelection(long orderId, String flagKey) {
        List<Status> statuses = statusRepository.findByOrdersOrderId(orderId);
        if (statuses == null || statuses.isEmpty()) {
            return new NestingSelectionResponse(List.of(), List.of(), List.of(), List.of());
        }

        List<String> designer = extractLatestSelectedRowIds(statuses, Department.DESIGN, flagKey);
        List<String> production = extractLatestSelectedRowIds(statuses, Department.PRODUCTION, flagKey);
        List<String> machining = extractLatestSelectedRowIds(statuses, Department.MACHINING, flagKey);
        List<String> inspection = extractLatestSelectedRowIds(statuses, Department.INSPECTION, flagKey);

        return new NestingSelectionResponse(designer, production, machining, inspection);
    }

    private List<String> extractLatestSelectedRowIds(List<Status> statuses, Department department, String flagKey) {
        Status latest = statuses.stream()
                .filter(s -> s != null
                        && s.getNewStatus() == department
                        && s.getComment() != null
                        && s.getComment().contains(flagKey)
                        && s.getComment().contains("selectedRowIds"))
                .max(Comparator.comparing(Status::getId))
                .orElse(null);

        if (latest == null || latest.getComment() == null || latest.getComment().isBlank()) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(latest.getComment().trim());
            JsonNode arr = node.get("selectedRowIds");
            if (arr == null || !arr.isArray()) return List.of();

            List<String> ids = new ArrayList<>();
            for (JsonNode n : arr) {
                if (n != null && !n.isNull()) {
                    String v = n.asText();
                    if (v != null && !v.isBlank()) ids.add(v);
                }
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }
}
