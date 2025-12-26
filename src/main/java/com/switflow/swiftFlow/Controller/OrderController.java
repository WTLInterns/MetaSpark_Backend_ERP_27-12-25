package com.switflow.swiftFlow.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.switflow.swiftFlow.Entity.User;
import com.switflow.swiftFlow.Repo.UserRepository;
import com.switflow.swiftFlow.Service.OrderService;
import com.switflow.swiftFlow.Request.OrderRequest;
import com.switflow.swiftFlow.Response.OrderResponse;
import com.switflow.swiftFlow.Response.DepartmentOrderCountResponse;
import com.switflow.swiftFlow.utility.Department;

@RequestMapping("/order")
@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create/{customerId}/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(@PathVariable int customerId, 
                                                     @PathVariable int productId,
                                                     @RequestBody OrderRequest orderRequest) {
        OrderResponse response = orderService.createOrder(orderRequest, customerId, productId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/getById/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getAll")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<OrderResponse>> getAllOrders(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));

        Department userDept = user.getDepartment();

        // Admin can see all orders; other departments are restricted to their own
        List<OrderResponse> response;
        if (userDept == Department.ADMIN) {
            response = orderService.getAllOrders();
        } else {
            // Fall back to department-based filtering for non-admin users
            response = orderService.getOrdersByDepartment(userDept.name());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getByDepartment/{department}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByDepartment(@PathVariable String department) {
        List<OrderResponse> response = orderService.getOrdersByDepartment(department);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getCountByDepartment")
    @PreAuthorize("hasAnyRole('ADMIN','DESIGN','PRODUCTION','MACHINING','INSPECTION')")
    public ResponseEntity<List<DepartmentOrderCountResponse>> getOrderCountByDepartment() {
        List<DepartmentOrderCountResponse> response = orderService.getOrderCountByDepartment();
        return ResponseEntity.ok(response);
    }
}