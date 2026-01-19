package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Request.NestingSelectionRequest;
import com.switflow.swiftFlow.Response.NestingSelectionResponse;
import com.switflow.swiftFlow.Service.NestingSelectionService;
import com.switflow.swiftFlow.utility.Department;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nesting")
public class NestingSelectionController {

    @Autowired
    private NestingSelectionService nestingSelectionService;

    // ---------------------------
    // ✅ SAVE Plate selection
    // ---------------------------
    @PostMapping("/order/{orderId}/plate-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> savePlateSelection(
            @PathVariable long orderId,
            @RequestBody NestingSelectionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Department role = resolveRole(httpRequest, request);

            List<String> selected = getSelected(role, request);

            if (role != Department.INSPECTION && (selected == null || selected.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("message", "No rows selected"));
            }

            nestingSelectionService.saveSelection(orderId, role, selected, "nestingPlateSelection");
            return ResponseEntity.ok(Map.of("message", "Plate selection saved successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save plate selection: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/order/{orderId}/plate-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<NestingSelectionResponse> getPlateSelection(@PathVariable long orderId) {
        return ResponseEntity.ok(nestingSelectionService.getSelection(orderId, "nestingPlateSelection"));
    }

    // ---------------------------
    // ✅ SAVE Part selection
    // ---------------------------
    @PostMapping("/order/{orderId}/part-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> savePartSelection(
            @PathVariable long orderId,
            @RequestBody NestingSelectionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Department role = resolveRole(httpRequest, request);

            List<String> selected = getSelected(role, request);

            if (role != Department.INSPECTION && (selected == null || selected.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("message", "No rows selected"));
            }

            nestingSelectionService.saveSelection(orderId, role, selected, "nestingPartSelection");
            return ResponseEntity.ok(Map.of("message", "Part selection saved successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save part selection: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/order/{orderId}/part-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<NestingSelectionResponse> getPartSelection(@PathVariable long orderId) {
        return ResponseEntity.ok(nestingSelectionService.getSelection(orderId, "nestingPartSelection"));
    }

    // ---------------------------
    // ✅ SAVE Result selection
    // ---------------------------
    @PostMapping("/order/{orderId}/result-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> saveResultSelection(
            @PathVariable long orderId,
            @RequestBody NestingSelectionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            Department role = resolveRole(httpRequest, request);

            List<String> selected = getSelected(role, request);

            if (role != Department.INSPECTION && (selected == null || selected.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of("message", "No rows selected"));
            }

            nestingSelectionService.saveSelection(orderId, role, selected, "nestingResultSelection");
            return ResponseEntity.ok(Map.of("message", "Result selection saved successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save result selection: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/order/{orderId}/result-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<NestingSelectionResponse> getResultSelection(@PathVariable long orderId) {
        return ResponseEntity.ok(nestingSelectionService.getSelection(orderId, "nestingResultSelection"));
    }

    // ======================================================
    // ✅ Helpers
    // ======================================================
    private List<String> getSelected(Department role, NestingSelectionRequest request) {
        if (role == Department.DESIGN) return request.getDesignerSelectedRowIds();
        if (role == Department.PRODUCTION) return request.getProductionSelectedRowIds();
        if (role == Department.MACHINING) return request.getMachineSelectedRowIds();
        return request.getInspectionSelectedRowIds();
    }

    private Department resolveRole(HttpServletRequest req, NestingSelectionRequest body) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authorityRole = null;
        if (authentication != null && authentication.getAuthorities() != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth != null && auth.startsWith("ROLE_")) {
                    authorityRole = auth;
                    break;
                }
            }
        }

        // Same logic like PartsSelectionController:
        String lower = "";
        if (req != null) {
            String referer = req.getHeader("Referer");
            if (referer != null) lower = referer.toLowerCase();
        }

        if (lower.contains("/designuser/") || "ROLE_DESIGN".equals(authorityRole)) return Department.DESIGN;
        if (lower.contains("/productionuser/") || "ROLE_PRODUCTION".equals(authorityRole)) return Department.PRODUCTION;
        if (lower.contains("/inspectionuser/") || "ROLE_INSPECTION".equals(authorityRole)) return Department.INSPECTION;
        if ("ROLE_MACHINING".equals(authorityRole)) return Department.MACHINING;

        // fallback: infer from body
        if (body != null) {
            if (body.getDesignerSelectedRowIds() != null && !body.getDesignerSelectedRowIds().isEmpty()) return Department.DESIGN;
            if (body.getProductionSelectedRowIds() != null && !body.getProductionSelectedRowIds().isEmpty()) return Department.PRODUCTION;
            if (body.getMachineSelectedRowIds() != null && !body.getMachineSelectedRowIds().isEmpty()) return Department.MACHINING;
            if (body.getInspectionSelectedRowIds() != null && !body.getInspectionSelectedRowIds().isEmpty()) return Department.INSPECTION;
        }

        return null;
    }
}
