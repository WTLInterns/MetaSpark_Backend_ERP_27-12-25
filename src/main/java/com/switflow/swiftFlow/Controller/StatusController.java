package com.switflow.swiftFlow.Controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.Service.StatusService;
import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Repo.UserRepository;
import com.switflow.swiftFlow.utility.Department;

@RestController
@RequestMapping("/status")
public class StatusController {

    @Autowired
    private StatusService statusService;

    @Autowired
    private UserRepository userRepository;


   
   
    @PostMapping("/create/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<StatusResponse> createStatus(
            @PathVariable long orderId, 
            @RequestPart("status") StatusRequest statusRequest,
            @RequestPart(value = "attachment", required = false) MultipartFile attachment) throws IOException {
        StatusResponse response = statusService.createStatus(statusRequest, orderId, attachment);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/create-with-attachment/{orderId}")
    // public ResponseEntity<StatusResponse> createStatusWithAttachment(
    //         @PathVariable long orderId,
    //         @RequestPart("status") StatusRequest statusRequest,
    //         @RequestPart(value = "attachment", required = false) MultipartFile attachment) throws IOException {
    //     StatusResponse response = statusService.createStatusWithAttachment(statusRequest, orderId, attachment);
    //     return ResponseEntity.ok(response);
    // }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<StatusResponse>> getStatusesByOrderId(@PathVariable long orderId,
            Authentication authentication) {

        List<StatusResponse> responses = statusService.getStatusesByOrderId(orderId);

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));

        Department userDept = user.getDepartment();

        if (userDept != Department.ADMIN) {
            Department finalDept = userDept;
            responses = responses.stream()
                    .filter(r -> r.getNewStatus() == finalDept)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/upload-pdf")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> uploadPdf(@RequestPart("file") MultipartFile file) {
        try {
            String url = statusService.uploadAttachment(file);
            return ResponseEntity.ok().body(java.util.Collections.singletonMap("attachmentUrl", url));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file");
        }
    }
}