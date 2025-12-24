package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.Service.PdfService;
import com.switflow.swiftFlow.Service.StatusService;
import com.switflow.swiftFlow.pdf.PdfRow;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pdf")
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @Autowired
    private StatusService statusService;

    @GetMapping("/order/{orderId}/rows")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN')")
    public ResponseEntity<List<PdfRow>> getPdfRows(@PathVariable long orderId) throws IOException {
        List<PdfRow> rows = pdfService.analyzePdfRows(orderId);
        return ResponseEntity.ok(rows);
    }

    public static class RowSelectionRequest {
        private List<String> selectedRowIds;

        public List<String> getSelectedRowIds() {
            return selectedRowIds;
        }

        public void setSelectedRowIds(List<String> selectedRowIds) {
            this.selectedRowIds = selectedRowIds;
        }
    }

    public static class RowSelectionStatusRequest extends RowSelectionRequest {
        private Department targetStatus;

        public Department getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(Department targetStatus) {
            this.targetStatus = targetStatus;
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
                    Department.PRODUCTION
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
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
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

            StatusResponse response = pdfService.saveRowSelection(
                    orderId,
                    request.getSelectedRowIds(),
                    Department.MACHINING
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
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("selectedRowIds", List.of()));
        }
    }
}
