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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
        private Boolean threeCheckbox; // optional, used for three-checkbox UI

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

        public Boolean getThreeCheckbox() {
            return threeCheckbox;
        }

        public void setThreeCheckbox(Boolean threeCheckbox) {
            this.threeCheckbox = threeCheckbox;
        }
    }

    public static class ThreeCheckboxRequest {
        private List<String> designerSelectedRowIds;
        private List<String> productionSelectedRowIds;
        private List<String> machineSelectedRowIds;
        private Long machineId;
        // optional marker to indicate this comes from new three-checkbox UI
        private Boolean threeCheckbox;

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

    @PostMapping("/order/{orderId}/inspection-selection")
    @PreAuthorize("hasAnyRole('ADMIN','MACHINING')")
    public ResponseEntity<?> saveInspectionSelection(
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

            StatusResponse response = pdfService.saveRowSelection(
                    orderId,
                    request.getSelectedRowIds(),
                    Department.INSPECTION,
                    null,
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
                    "message", "Failed to send to Inspection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
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

    @PostMapping("/order/{orderId}/three-checkbox-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING')")
    public ResponseEntity<?> saveThreeCheckboxSelection(
            @PathVariable long orderId,
            @RequestBody ThreeCheckboxRequest request
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Invalid request",
                        "status", 400
                ));
            }

            // Determine caller role from Spring Security
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String role = null;
            if (authentication != null && authentication.getAuthorities() != null) {
                for (GrantedAuthority authority : authentication.getAuthorities()) {
                    String auth = authority.getAuthority();
                    if (auth != null && auth.startsWith("ROLE_")) {
                        role = auth;
                        break;
                    }
                }
            }

            boolean isAdmin = "ROLE_ADMIN".equals(role);
            boolean isDesign = "ROLE_DESIGN".equals(role);
            boolean isProduction = "ROLE_PRODUCTION".equals(role);
            boolean isMachining = "ROLE_MACHINING".equals(role);

            // Save designer selection – only DESIGN or ADMIN may write this
            if ((isDesign || isAdmin)
                    && request.getDesignerSelectedRowIds() != null
                    && !request.getDesignerSelectedRowIds().isEmpty()) {
                pdfService.saveRowSelectionWithoutTransition(
                        orderId,
                        request.getDesignerSelectedRowIds(),
                        Department.DESIGN,
                        null,
                        null,
                        null
                );
            }

            // Save production selection – only PRODUCTION or ADMIN may write this
            if ((isProduction || isAdmin)
                    && request.getProductionSelectedRowIds() != null
                    && !request.getProductionSelectedRowIds().isEmpty()) {
                pdfService.saveRowSelectionWithoutTransition(
                        orderId,
                        request.getProductionSelectedRowIds(),
                        Department.PRODUCTION,
                        null,
                        null,
                        null
                );
            }

            // Save machine selection – only MACHINING or ADMIN may write this
            if ((isMachining || isAdmin)
                    && request.getMachineSelectedRowIds() != null
                    && !request.getMachineSelectedRowIds().isEmpty()) {
                String machineName = null;
                if (request.getMachineId() != null) {
                    try {
                        MachinesResponse machine = machinesService.getMachines(request.getMachineId().intValue());
                        if (machine != null) {
                            machineName = machine.getMachineName();
                        }
                    } catch (Exception ignored) {
                    }
                }

                pdfService.saveRowSelectionWithoutTransition(
                        orderId,
                        request.getMachineSelectedRowIds(),
                        Department.MACHINING,
                        null,
                        request.getMachineId(),
                        machineName
                );
            }

            return ResponseEntity.ok(Map.of("message", "Three-checkbox selection saved successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save three-checkbox selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/three-checkbox-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING')")
    public ResponseEntity<Map<String, Object>> getThreeCheckboxSelection(@PathVariable long orderId) {
        List<StatusResponse> history = statusService.getStatusesByOrderId(orderId);
        Map<String, Object> result = new HashMap<>();

        if (history == null || history.isEmpty()) {
            result.put("designerSelectedRowIds", List.of());
            result.put("productionSelectedRowIds", List.of());
            result.put("machineSelectedRowIds", List.of());
            return ResponseEntity.ok(result);
        }

        // Get designer selection
        StatusResponse designerLatest = history.stream()
                .filter(s -> s.getNewStatus() == Department.DESIGN
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .reduce((first, second) -> second)
                .orElse(null);

        if (designerLatest != null) {
            List<String> designerIds = extractRowIdsFromComment(designerLatest.getComment());
            result.put("designerSelectedRowIds", designerIds);
        } else {
            result.put("designerSelectedRowIds", List.of());
        }

        // Get production selection
        StatusResponse productionLatest = history.stream()
                .filter(s -> s.getNewStatus() == Department.PRODUCTION
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .reduce((first, second) -> second)
                .orElse(null);

        if (productionLatest != null) {
            List<String> productionIds = extractRowIdsFromComment(productionLatest.getComment());
            result.put("productionSelectedRowIds", productionIds);
        } else {
            result.put("productionSelectedRowIds", List.of());
        }

        // Get machine selection – only consider entries created by the
        // three-checkbox flow; otherwise return an empty list so no
        // predefined ticks are shown in the Machining UI.
        StatusResponse machineLatest = history.stream()
                .filter(s -> s.getNewStatus() == Department.MACHINING
                        && s.getComment() != null
                        && s.getComment().contains("selectedRowIds"))
                .reduce((first, second) -> second)
                .orElse(null);

        if (machineLatest != null) {
            try {
                JsonNode node = objectMapper.readTree(machineLatest.getComment());
                // If a threeCheckbox flag is present and true, treat this as a
                // valid machine selection for the new UI. Otherwise ignore it
                // and report an empty list so there are no predefined ticks.
                JsonNode marker = node.get("threeCheckbox");
                boolean isThreeCheckbox = marker != null && marker.asBoolean(false);
                if (isThreeCheckbox) {
                    List<String> machineIds = extractRowIdsFromComment(machineLatest.getComment());
                    result.put("machineSelectedRowIds", machineIds);

                    JsonNode machineIdNode = node.get("machineId");
                    if (machineIdNode != null && !machineIdNode.isNull()) {
                        result.put("machineId", machineIdNode.asLong());
                    }
                    JsonNode machineNameNode = node.get("machineName");
                    if (machineNameNode != null && !machineNameNode.isNull()) {
                        result.put("machineName", machineNameNode.asText());
                    }
                } else {
                    // Not created by new three-checkbox flow – ignore
                    result.put("machineSelectedRowIds", List.of());
                }
            } catch (Exception e) {
                result.put("machineSelectedRowIds", List.of());
            }
        } else {
            result.put("machineSelectedRowIds", List.of());
        }

        return ResponseEntity.ok(result);
    }

    private List<String> extractRowIdsFromComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return List.of();
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
            return ids;
        } catch (Exception e) {
            // Fallback parsing
            try {
                int start = comment.indexOf('[');
                int end = comment.indexOf(']');
                if (start == -1 || end == -1 || end <= start) {
                    return List.of();
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
                return ids;
            } catch (Exception fallback) {
                return List.of();
            }
        }
    }
}
