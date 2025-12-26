package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Response.StatusResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.switflow.swiftFlow.Service.PdfService;
import com.switflow.swiftFlow.Service.StatusService;
import com.switflow.swiftFlow.Service.MachinesService;
import com.switflow.swiftFlow.Response.MachinesResponse;
import com.switflow.swiftFlow.pdf.PdfRow;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private MachinesService machinesService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/order/{orderId}/rows")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN')")
    public ResponseEntity<List<PdfRow>> getPdfRows(@PathVariable long orderId) throws IOException {
        List<PdfRow> rows = pdfService.analyzePdfRows(orderId);
        return ResponseEntity.ok(rows);
    }

    public static class RowSelectionRequest {
        private List<String> selectedRowIds;
        private Long machineId; // optional, used for machining-selection

        public List<String> getSelectedRowIds() {
            return selectedRowIds;
        }

        public void setSelectedRowIds(List<String> selectedRowIds) {
            this.selectedRowIds = selectedRowIds;
        }

        public Long getMachineId() {
            return machineId;
        }

        public void setMachineId(Long machineId) {
            this.machineId = machineId;
        }
    }

    public static class RowSelectionStatusRequest extends RowSelectionRequest {
        private Department targetStatus;

        // Optional explicit attachmentUrl for PRODUCTION / PRODUCTION_READY status
        private String attachmentUrl;

        public Department getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(Department targetStatus) {
            this.targetStatus = targetStatus;
        }

        public String getAttachmentUrl() {
            return attachmentUrl;
        }

        public void setAttachmentUrl(String attachmentUrl) {
            this.attachmentUrl = attachmentUrl;
        }
    }

    @PostMapping("/order/{orderId}/filter")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN')")
    public ResponseEntity<?> createFilteredPdf(
            @PathVariable long orderId,
            @RequestBody RowSelectionRequest request
    ) {
        if (request == null || request.getSelectedRowIds() == null || request.getSelectedRowIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "No rows selected",
                    "status", 400
            ));
        }

        try {
            StatusResponse response = pdfService.generateFilteredPdf(orderId, request.getSelectedRowIds());
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to generate filtered PDF: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @PostMapping("/order/{orderId}/selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION')")
    public ResponseEntity<?> saveRowSelection(
            @PathVariable long orderId,
            @RequestBody RowSelectionStatusRequest request
    ) {
        try {
            if (request == null || request.getSelectedRowIds() == null || request.getSelectedRowIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No rows selected",
                        "status", 400
                ));
            }

            StatusResponse response = pdfService.saveRowSelection(
                    orderId,
                    request.getSelectedRowIds(),
                    Department.PRODUCTION,
                    request.getAttachmentUrl(),
                    null,
                    null
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save PDF row selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION')")
    public ResponseEntity<Map<String, Object>> getRowSelection(@PathVariable long orderId) {
        List<StatusResponse> history = statusService.getStatusesByOrderId(orderId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        StatusResponse latest = history.stream()
                .filter(s -> s.getNewStatus() == Department.PRODUCTION
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .reduce((first, second) -> second)
                .orElse(null);

        if (latest == null) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        String comment = latest.getComment();
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(comment);

            List<String> ids = new ArrayList<>();
            JsonNode arr = root.get("selectedRowIds");
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) {
                    ids.add(n.asText());
                }
            }

            Long machineId = null;
            String machineName = null;
            if (root.has("machineId")) {
                machineId = root.get("machineId").isNull() ? null : root.get("machineId").asLong();
            }
            if (root.has("machineName")) {
                machineName = root.get("machineName").isNull() ? null : root.get("machineName").asText();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("selectedRowIds", ids);
            if (machineId != null) {
                result.put("machineId", machineId);
            }
            if (machineName != null) {
                result.put("machineName", machineName);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Fallback: try old string parsing for selectedRowIds only
            try {
                int start = comment.indexOf('[');
                int end = comment.indexOf(']');
                if (start == -1 || end == -1 || end <= start) {
                    return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
                }
                String inside = comment.substring(start + 1, end);
                String[] parts = inside.split(",");
                List<String> ids = new ArrayList<>();
                for (String p : parts) {
                    String id = p.trim();
                    if (id.startsWith("\"")) {
                        id = id.substring(1);
                    }
                    if (id.endsWith("\"")) {
                        id = id.substring(0, id.length() - 1);
                    }
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
                return ResponseEntity.ok(Map.of("selectedRowIds", ids));
            } catch (Exception fallback) {
                return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
            }
        }
    }

    @PostMapping("/order/{orderId}/machining-selection")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION','MACHINING')")
    public ResponseEntity<?> saveMachiningSelection(
            @PathVariable long orderId,
            @RequestBody RowSelectionRequest request
    ) {
        try {
            if (request == null || request.getSelectedRowIds() == null || request.getSelectedRowIds().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No rows selected",
                        "status", 400
                ));
            }
            Long machineId = request.getMachineId();
            String machineName = null;
            if (machineId != null) {
                try {
                    MachinesResponse machine = machinesService.getMachines(machineId.intValue());
                    if (machine != null) {
                        machineName = machine.getMachineName();
                    }
                } catch (Exception ignored) {
                }
            }

            StatusResponse response = pdfService.saveRowSelection(
                    orderId,
                    request.getSelectedRowIds(),
                    Department.MACHINING,
                    null,
                    machineId,
                    machineName
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save machining selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/machining-selection")
    @PreAuthorize("hasAnyRole('ADMIN','PRODUCTION','MACHINING')")
    public ResponseEntity<Map<String, Object>> getMachiningSelection(@PathVariable long orderId) {
        List<StatusResponse> history = statusService.getStatusesByOrderId(orderId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        StatusResponse latest = history.stream()
                .filter(s -> s.getNewStatus() == Department.MACHINING
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .reduce((first, second) -> second)
                .orElse(null);

        if (latest == null) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }

        String comment = latest.getComment();
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }
        try {
            JsonNode node = objectMapper.readTree(comment);
            JsonNode selectedArray = node.get("selectedRowIds");
            List<String> ids = new ArrayList<>();
            if (selectedArray != null && selectedArray.isArray()) {
                for (JsonNode idNode : selectedArray) {
                    String id = idNode.asText();
                    if (id != null && !id.isBlank()) {
                        ids.add(id);
                    }
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("selectedRowIds", ids);
            JsonNode machineIdNode = node.get("machineId");
            if (machineIdNode != null && !machineIdNode.isNull()) {
                result.put("machineId", machineIdNode.asLong());
            }
            JsonNode machineNameNode = node.get("machineName");
            if (machineNameNode != null && !machineNameNode.isNull()) {
                result.put("machineName", machineNameNode.asText());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Fallback: try old parsing logic for selectedRowIds only
            try {
                int start = comment.indexOf('[');
                int end = comment.indexOf(']');
                if (start == -1 || end == -1 || end <= start) {
                    return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
                }
                String inside = comment.substring(start + 1, end);
                String[] parts = inside.split(",");
                List<String> ids = new ArrayList<>();
                for (String p : parts) {
                    String id = p.trim();
                    if (id.startsWith("\"")) {
                        id = id.substring(1);
                    }
                    if (id.endsWith("\"")) {
                        id = id.substring(0, id.length() - 1);
                    }
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
                return ResponseEntity.ok(Map.of("selectedRowIds", ids));
            } catch (Exception fallback) {
                return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
            }
        }
    }
}
