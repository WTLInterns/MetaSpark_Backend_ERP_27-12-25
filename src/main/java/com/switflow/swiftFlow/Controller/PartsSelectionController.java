package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Request.PartsSelectionRequest;
import com.switflow.swiftFlow.Response.PartsSelectionResponse;
import com.switflow.swiftFlow.Service.PartsSelectionService;
import com.switflow.swiftFlow.utility.Department;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pdf")
public class PartsSelectionController {

    @Autowired
    private PartsSelectionService partsSelectionService;

    @PostMapping("/order/{orderId}/parts-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> savePartsSelection(
            @PathVariable long orderId,
            @RequestBody PartsSelectionRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Invalid request",
                        "status", 400
                ));
            }

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

            Department currentRole = resolveEffectiveRole(httpRequest, request, authorityRole);
            if (currentRole == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Unable to determine role",
                        "status", 400
                ));
            }

            List<String> selected;
            if (currentRole == Department.DESIGN) {
                selected = request.getDesignerSelectedRowIds();
            } else if (currentRole == Department.PRODUCTION) {
                selected = request.getProductionSelectedRowIds();
            } else if (currentRole == Department.MACHINING) {
                selected = request.getMachineSelectedRowIds();
            } else {
                selected = request.getInspectionSelectedRowIds();
            }

            if (currentRole != Department.INSPECTION && (selected == null || selected.isEmpty())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "No rows selected",
                        "status", 400
                ));
            }

            partsSelectionService.saveSelection(orderId, currentRole, selected);
            return ResponseEntity.ok(Map.of("message", "Parts selection saved successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", ex.getMessage(),
                    "status", 400
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "message", "Failed to save parts selection: " + e.getMessage(),
                    "error", e.getClass().getSimpleName(),
                    "status", 500
            ));
        }
    }

    @GetMapping("/order/{orderId}/parts-selection")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<PartsSelectionResponse> getPartsSelection(@PathVariable long orderId) {
        return ResponseEntity.ok(partsSelectionService.getSelection(orderId));
    }

    private Department resolveEffectiveRole(HttpServletRequest req, PartsSelectionRequest body, String authorityRole) {
        boolean isAdmin = "ROLE_ADMIN".equals(authorityRole);

        int bodyNonEmptyCount = 0;
        Department bodyInferred = null;
        if (body != null) {
            if (body.getDesignerSelectedRowIds() != null && !body.getDesignerSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.DESIGN;
            }
            if (body.getProductionSelectedRowIds() != null && !body.getProductionSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.PRODUCTION;
            }
            if (body.getMachineSelectedRowIds() != null && !body.getMachineSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.MACHINING;
            }
            if (body.getInspectionSelectedRowIds() != null && !body.getInspectionSelectedRowIds().isEmpty()) {
                bodyNonEmptyCount++;
                bodyInferred = Department.INSPECTION;
            }
        }

        if (isAdmin) {
            if (bodyNonEmptyCount != 1) {
                throw new IllegalArgumentException("ADMIN must provide exactly one non-empty role selection array");
            }
            return bodyInferred;
        }

        if (bodyNonEmptyCount == 1) {
            return bodyInferred;
        }

        Department fromContext = resolveRoleFromRequestContext(req);
        if (fromContext != null) {
            return fromContext;
        }

        if ("ROLE_DESIGN".equals(authorityRole)) {
            return Department.DESIGN;
        }
        if ("ROLE_PRODUCTION".equals(authorityRole)) {
            return Department.PRODUCTION;
        }
        if ("ROLE_MACHINING".equals(authorityRole)) {
            return Department.MACHINING;
        }
        if ("ROLE_INSPECTION".equals(authorityRole)) {
            return Department.INSPECTION;
        }
        return null;
    }

    private Department resolveRoleFromRequestContext(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String referer = request.getHeader("Referer");
        String forwardedUri = request.getHeader("X-Forwarded-Uri");
        String originalUri = request.getHeader("X-Original-URI");
        String originalUrl = request.getHeader("X-Original-URL");

        String context = (referer != null ? referer : "")
                + " " + (forwardedUri != null ? forwardedUri : "")
                + " " + (originalUri != null ? originalUri : "")
                + " " + (originalUrl != null ? originalUrl : "");
        String lower = context.toLowerCase();

        if (lower.contains("/designuser/")) {
            return Department.DESIGN;
        }
        if (lower.contains("/productionuser/")) {
            return Department.PRODUCTION;
        }
        if (lower.contains("/mechanistuser/") || lower.contains("/mechanicuser/") || lower.contains("/machinistuser/")) {
            return Department.MACHINING;
        }
        if (lower.contains("/inspectionuser/")) {
            return Department.INSPECTION;
        }
        return null;
    }
}
