package com.switflow.swiftFlow.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.switflow.swiftFlow.Response.SubNestRowDto;
import com.switflow.swiftFlow.Response.PartsRowDto;
import com.switflow.swiftFlow.Response.MaterialDataRowDto;
import com.switflow.swiftFlow.Service.PdfSubnestService;

@RestController
@RequestMapping("/api/pdf/subnest")
public class PdfSubnestController {

    @Autowired
    private PdfSubnestService pdfSubnestService;

    @GetMapping("/mock")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<SubNestRowDto>> getMockSubnestRows() {
        return ResponseEntity.ok(pdfSubnestService.getMockSubnestRows());
    }

    @GetMapping("/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<SubNestRowDto>> getSubnestByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parseSubnestFromUrl(attachmentUrl));
    }

    @GetMapping("/parts/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<PartsRowDto>> getPartsByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parsePartsFromUrl(attachmentUrl));
    }

    @GetMapping("/material-data/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<MaterialDataRowDto>> getMaterialDataByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parseMaterialDataFromUrl(attachmentUrl));
    }

    @GetMapping("/debug-text")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<String> getPdfRawText(@RequestParam("attachmentUrl") String attachmentUrl) throws Exception {
        String text = pdfSubnestService.extractRawTextFromUrl(attachmentUrl);
        return ResponseEntity.ok(text);
    }

    // ========== NEW ENDPOINTS FOR NESTING PDF FORMAT ==========

    @GetMapping("/nesting/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<SubNestRowDto>> getNestingSubnestByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parseNestingSubnestFromUrl(attachmentUrl));
    }

    @GetMapping("/nesting/parts/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<PartsRowDto>> getNestingPartsByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parseNestingPartsFromUrl(attachmentUrl));
    }

    @GetMapping("/nesting/material-data/by-url")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<MaterialDataRowDto>> getNestingMaterialDataByUrl(@RequestParam("attachmentUrl") String attachmentUrl) {
        return ResponseEntity.ok(pdfSubnestService.parseNestingMaterialDataFromUrl(attachmentUrl));
    }
}
