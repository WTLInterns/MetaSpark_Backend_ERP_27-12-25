package com.switflow.swiftFlow.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.switflow.swiftFlow.Response.PlateInfoRowDto;
import com.switflow.swiftFlow.Response.PartInfoRowDto;
import com.switflow.swiftFlow.Response.ResultBlockDto;
import com.switflow.swiftFlow.Service.NestingPdfService;

@RestController
@RequestMapping("/api/nesting")
public class NestingPdfController {

    @Autowired
    private NestingPdfService nestingPdfService;

    // ✅ Plate Info
    @GetMapping("/plate-info")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<PlateInfoRowDto>> getPlateInfo(
            @RequestParam("attachmentUrl") String attachmentUrl) {

        List<PlateInfoRowDto> rows = nestingPdfService.parsePlateInfo(attachmentUrl);
        return ResponseEntity.ok(rows);
    }

    // ✅ Part Info
    @GetMapping("/part-info")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<PartInfoRowDto>> getPartInfo(
            @RequestParam("attachmentUrl") String attachmentUrl) {

        List<PartInfoRowDto> rows = nestingPdfService.parsePartInfo(attachmentUrl);
        return ResponseEntity.ok(rows);
    }

    // ✅ Results
    @GetMapping("/results")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<ResultBlockDto>> getResults(
            @RequestParam("attachmentUrl") String attachmentUrl) {

        List<ResultBlockDto> results = nestingPdfService.parseResults(attachmentUrl);
        return ResponseEntity.ok(results);
    }
}
