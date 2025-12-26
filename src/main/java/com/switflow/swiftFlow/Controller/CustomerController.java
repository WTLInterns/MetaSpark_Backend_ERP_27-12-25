package com.switflow.swiftFlow.Controller;

import com.switflow.swiftFlow.Exception.CustomerEmailAlreadyExistsException;
import com.switflow.swiftFlow.Request.CustomerRequest;
import com.switflow.swiftFlow.Response.CustomerResposne;
import com.switflow.swiftFlow.Response.MessageResponse;
import com.switflow.swiftFlow.Service.CustomerService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Customer Controller
 */
@RestController
@RequestMapping("/customer")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCustomer(@RequestBody CustomerRequest customerRequest) {
        try {
            CustomerResposne response = customerService.createCustomer(customerRequest);
            return ResponseEntity.ok(response);
        } catch (CustomerEmailAlreadyExistsException e) {
            return ResponseEntity.status(409).body(new MessageResponse(e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/upload-excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadCustomersFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            MessageResponse response = customerService.importCustomersFromExcel(file);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping({"/all", "/getAll"})
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<?> getAllCustomers() {
        try {
            List<CustomerResposne> response = this.customerService.getAllCustomers();
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @GetMapping("/get/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCustomerById(@PathVariable int id) {
        try {
            CustomerResposne response = customerService.getCustomerById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCustomer(@PathVariable int id, @RequestBody CustomerRequest customerRequest) {
        try {
            CustomerResposne response = customerService.updateCustomer(id, customerRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCustomer(@PathVariable int customerId) {
        try {
            MessageResponse response = customerService.deleteCustomer(customerId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}