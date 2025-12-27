package com.switflow.swiftFlow.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.switflow.swiftFlow.Entity.Customer;
import com.switflow.swiftFlow.Entity.Orders;
import com.switflow.swiftFlow.Entity.Product;
import com.switflow.swiftFlow.Repo.CustomerRepo;
import com.switflow.swiftFlow.Repo.OrderRepository;
import com.switflow.swiftFlow.Repo.ProductRepository;
import com.switflow.swiftFlow.Request.OrderRequest;
import com.switflow.swiftFlow.Response.OrderResponse;
import com.switflow.swiftFlow.Response.OrderResponse.CustomerInfo;
import com.switflow.swiftFlow.Response.OrderResponse.ProductInfo;
import com.switflow.swiftFlow.Response.DepartmentOrderCountResponse;
import com.switflow.swiftFlow.Exception.OrderNotFoundException;
import com.switflow.swiftFlow.utility.Department;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepo customerRepository;

    @Autowired
    private ProductRepository productRepository;

    public OrderResponse createOrder(OrderRequest orderRequest, int customerId, int productId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new OrderNotFoundException("Customer not found with ID: " + customerId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new OrderNotFoundException("Product not found with ID: " + productId));

        // Check if customer and product are active
        StringBuilder errorMessage = new StringBuilder();
        boolean hasError = false;

        if (!"Active".equals(customer.getStatus())) {
            errorMessage.append("Customer is Inactive");
            hasError = true;
        }

        if (!"Active".equals(product.getStatus())) {
            if (hasError) {
                errorMessage.append(" and ");
            }
            errorMessage.append("Product is Inactive");
            hasError = true;
        }

        if (hasError) {
            throw new OrderNotFoundException(errorMessage.toString());
        }

        Orders order = new Orders();
        order.setProductDetails(orderRequest.getProductDetails());
        order.setCustomProductDetails(orderRequest.getCustomProductDetails());
        order.setUnits(orderRequest.getUnits());
        order.setMaterial(orderRequest.getMaterial());
        order.setDepartment(orderRequest.getDepartment()); // Add department to the order
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        order.setDateAdded(LocalDate.now().format(formatter));
        order.setStatus("Active");

        
        Orders savedOrder = orderRepository.save(order);
        
        List<Orders> customerOrders = customer.getOrders();
        if (customerOrders == null) {
            customerOrders = new ArrayList<>();
        }
        customerOrders.add(savedOrder);
        customer.setOrders(customerOrders);
        customerRepository.save(customer);
        
        List<Orders> productOrders = product.getOrders();
        if (productOrders == null) {
            productOrders = new ArrayList<>();
        }
        productOrders.add(savedOrder);
        product.setOrders(productOrders);
        productRepository.save(product);
        
        // Use mutable ArrayList instead of Arrays.asList() to avoid UnsupportedOperationException
        savedOrder.setCustomers(new ArrayList<>(Arrays.asList(customer)));
        savedOrder.setProducts(new ArrayList<>(Arrays.asList(product)));
        savedOrder = orderRepository.save(savedOrder);

        OrderResponse response = new OrderResponse();
        response.setOrderId(savedOrder.getOrderId());
        response.setProductDetails(savedOrder.getProductDetails());
        response.setCustomProductDetails(savedOrder.getCustomProductDetails());
        response.setUnits(savedOrder.getUnits());
        response.setMaterial(savedOrder.getMaterial());
        response.setStatus(savedOrder.getStatus());
        response.setDateAdded(savedOrder.getDateAdded());
        
        List<CustomerInfo> customerInfos = new ArrayList<>();
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setCustomerId(customer.getCustomerId());
        customerInfo.setCustomerName(customer.getCustomerName());
        customerInfo.setCompanyName(customer.getCompanyName());
        customerInfo.setCustomerEmail(customer.getCustomerEmail());
        customerInfo.setCustomerPhone(customer.getCustomerPhone());
        customerInfos.add(customerInfo);
        response.setCustomers(customerInfos);
        
        List<ProductInfo> productInfos = new ArrayList<>();
        ProductInfo productInfo = new ProductInfo();
        productInfo.setProductId(product.getId());
        productInfo.setProductCode(product.getProductCode());
        productInfo.setProductName(product.getProductName());
        productInfos.add(productInfo);
        response.setProducts(productInfos);

        return response;
    }

    
    
    public OrderResponse getOrderById(Long orderId) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setProductDetails(order.getProductDetails());
        response.setCustomProductDetails(order.getCustomProductDetails());
        response.setUnits(order.getUnits());
        response.setMaterial(order.getMaterial());
        response.setStatus(order.getStatus());
        response.setDateAdded(order.getDateAdded());
        response.setDepartment(order.getDepartment()); // Add department to response
        
        if (order.getCustomers() != null && !order.getCustomers().isEmpty()) {
            List<CustomerInfo> customerInfos = order.getCustomers().stream()
                .map(customer -> {
                    CustomerInfo info = new CustomerInfo();
                    info.setCustomerId(customer.getCustomerId());
                    info.setCustomerName(customer.getCustomerName());
                    info.setCompanyName(customer.getCompanyName());
                    info.setCustomerEmail(customer.getCustomerEmail());
                    info.setCustomerPhone(customer.getCustomerPhone());
                    return info;
                })
                .collect(Collectors.toList());
            response.setCustomers(customerInfos);
        }
        
        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            List<ProductInfo> productInfos = order.getProducts().stream()
                .map(product -> {
                    ProductInfo info = new ProductInfo();
                    info.setProductId(product.getId());
                    info.setProductCode(product.getProductCode());
                    info.setProductName(product.getProductName());
                    return info;
                })
                .collect(Collectors.toList());
            response.setProducts(productInfos);
        }
        
        return response;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
            .map(order -> {
                OrderResponse response = new OrderResponse();
                response.setOrderId(order.getOrderId());
                response.setProductDetails(order.getProductDetails());
                response.setCustomProductDetails(order.getCustomProductDetails());
                response.setUnits(order.getUnits());
                response.setMaterial(order.getMaterial());
                response.setStatus(order.getStatus());
                response.setDateAdded(order.getDateAdded());
                response.setDepartment(order.getDepartment()); // Add department to response
                
                // Map customers if they exist
                if (order.getCustomers() != null && !order.getCustomers().isEmpty()) {
                    List<CustomerInfo> customerInfos = order.getCustomers().stream()
                        .map(customer -> {
                            CustomerInfo info = new CustomerInfo();
                            info.setCustomerId(customer.getCustomerId());
                            info.setCustomerName(customer.getCustomerName());
                            info.setCompanyName(customer.getCompanyName());
                            info.setCustomerEmail(customer.getCustomerEmail());
                            info.setCustomerPhone(customer.getCustomerPhone());
                            info.setPrimaryAddress(customer.getPrimaryAddress());
                            info.setBillingAddress(customer.getBillingAddress());
                            info.setShippingAddress(customer.getShippingAddress());
                            
                            return info;
                        })
                        .collect(Collectors.toList());
                    response.setCustomers(customerInfos);
                }
                
                // Map products if they exist
                if (order.getProducts() != null && !order.getProducts().isEmpty()) {
                    List<ProductInfo> productInfos = order.getProducts().stream()
                        .map(product -> {
                            ProductInfo info = new ProductInfo();
                            info.setProductId(product.getId());
                            info.setProductCode(product.getProductCode());
                            info.setProductName(product.getProductName());
                            return info;
                        })
                        .collect(Collectors.toList());
                    response.setProducts(productInfos);
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }


    public List<OrderResponse> getOrdersByDepartment(String department) {
        // Convert string to Department enum
        Department dept = Department.valueOf(department.toUpperCase());
        
        return orderRepository.findByDepartment(dept).stream()
            .map(order -> {
                OrderResponse response = new OrderResponse();
                response.setOrderId(order.getOrderId());
                response.setProductDetails(order.getProductDetails());
                response.setCustomProductDetails(order.getCustomProductDetails());
                response.setUnits(order.getUnits());
                response.setMaterial(order.getMaterial());
                response.setStatus(order.getStatus());
                response.setDateAdded(order.getDateAdded());
                response.setDepartment(order.getDepartment());
                
                // Map customers if they exist
                if (order.getCustomers() != null && !order.getCustomers().isEmpty()) {
                    List<CustomerInfo> customerInfos = order.getCustomers().stream()
                        .map(customer -> {
                            CustomerInfo info = new CustomerInfo();
                            info.setCustomerId(customer.getCustomerId());
                            info.setCustomerName(customer.getCustomerName());
                            info.setCompanyName(customer.getCompanyName());
                            info.setCustomerEmail(customer.getCustomerEmail());
                            info.setCustomerPhone(customer.getCustomerPhone());
                            return info;
                        })
                        .collect(Collectors.toList());
                    response.setCustomers(customerInfos);
                }
                
                // Map products if they exist
                if (order.getProducts() != null && !order.getProducts().isEmpty()) {
                    List<ProductInfo> productInfos = order.getProducts().stream()
                        .map(product -> {
                            ProductInfo info = new ProductInfo();
                            info.setProductId(product.getId());
                            info.setProductCode(product.getProductCode());
                            info.setProductName(product.getProductName());
                            return info;
                        })
                        .collect(Collectors.toList());
                    response.setProducts(productInfos);
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get count of orders for each department
     * @return List of department names and their order counts
     */
    public List<DepartmentOrderCountResponse> getOrderCountByDepartment() {
        return Arrays.stream(Department.values())
            .filter(dept -> dept != Department.ADMIN) // Exclude ADMIN department
            .map(dept -> new DepartmentOrderCountResponse(dept.name(), orderRepository.findByDepartment(dept).size()))
            .collect(Collectors.toList());
    }
    
    /**
     * Update an existing order
     * @param orderId The ID of the order to update
     * @param orderRequest The updated order details
     * @return The updated order response
     */
    @Transactional
    public OrderResponse updateOrder(Long orderId, OrderRequest orderRequest) {
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
        
        // Store old customer and product IDs for cleanup
        Integer oldCustomerId = null;
        Integer oldProductId = null;
        
        if (order.getCustomers() != null && !order.getCustomers().isEmpty()) {
            oldCustomerId = order.getCustomers().get(0).getCustomerId();
        }
        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            oldProductId = order.getProducts().get(0).getId();
        }
        
        // Update order fields
        order.setCustomProductDetails(orderRequest.getCustomProductDetails());
        order.setUnits(orderRequest.getUnits());
        order.setMaterial(orderRequest.getMaterial());
        order.setDepartment(orderRequest.getDepartment());
        
        // Handle customer relationship update
        if (orderRequest.getCustomerId() != null) {
            Customer newCustomer = customerRepository.findById(orderRequest.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + orderRequest.getCustomerId()));
            
            // Remove order from old customer's orders list if different
            if (oldCustomerId != null && !oldCustomerId.equals(orderRequest.getCustomerId())) {
                Customer oldCustomer = customerRepository.findById(oldCustomerId).orElse(null);
                if (oldCustomer != null && oldCustomer.getOrders() != null) {
                    oldCustomer.getOrders().removeIf(o -> o.getOrderId().equals(orderId));
                    customerRepository.save(oldCustomer);
                }
            }
            
            // Add order to new customer's orders list if not already present
            if (newCustomer.getOrders() == null) {
                newCustomer.setOrders(new ArrayList<>());
            }
            if (!newCustomer.getOrders().stream().anyMatch(o -> o.getOrderId().equals(orderId))) {
                newCustomer.getOrders().add(order);
                customerRepository.save(newCustomer);
            }
            
            // Update order's customer reference
            order.getCustomers().clear();
            order.getCustomers().add(newCustomer);
        } else {
            // Clear customer relationship
            if (oldCustomerId != null) {
                Customer oldCustomer = customerRepository.findById(oldCustomerId).orElse(null);
                if (oldCustomer != null && oldCustomer.getOrders() != null) {
                    oldCustomer.getOrders().removeIf(o -> o.getOrderId().equals(orderId));
                    customerRepository.save(oldCustomer);
                }
            }
            order.getCustomers().clear();
        }
        
        // Handle product relationship update
        if (orderRequest.getProductId() != null) {
            Product newProduct = productRepository.findById(orderRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with ID: " + orderRequest.getProductId()));
            
            // Remove order from old product's orders list if different
            if (oldProductId != null && !oldProductId.equals(orderRequest.getProductId())) {
                Product oldProduct = productRepository.findById(oldProductId).orElse(null);
                if (oldProduct != null && oldProduct.getOrders() != null) {
                    oldProduct.getOrders().removeIf(o -> o.getOrderId().equals(orderId));
                    productRepository.save(oldProduct);
                }
            }
            
            // Add order to new product's orders list if not already present
            if (newProduct.getOrders() == null) {
                newProduct.setOrders(new ArrayList<>());
            }
            if (!newProduct.getOrders().stream().anyMatch(o -> o.getOrderId().equals(orderId))) {
                newProduct.getOrders().add(order);
                productRepository.save(newProduct);
            }
            
            // Update order's product reference
            order.getProducts().clear();
            order.getProducts().add(newProduct);
        } else {
            // Clear product relationship
            if (oldProductId != null) {
                Product oldProduct = productRepository.findById(oldProductId).orElse(null);
                if (oldProduct != null && oldProduct.getOrders() != null) {
                    oldProduct.getOrders().removeIf(o -> o.getOrderId().equals(orderId));
                    productRepository.save(oldProduct);
                }
            }
            order.getProducts().clear();
        }
        
        // Save updated order
        Orders updatedOrder = orderRepository.save(order);
        
        return convertToOrderResponse(updatedOrder);
    }
    
    /**
     * Helper method to convert Orders entity to OrderResponse
     * @param order The order entity
     * @return The order response
     */
    private OrderResponse convertToOrderResponse(Orders order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getOrderId());
        response.setProductDetails(order.getProductDetails());
        response.setCustomProductDetails(order.getCustomProductDetails());
        response.setUnits(order.getUnits());
        response.setMaterial(order.getMaterial());
        response.setStatus(order.getStatus());
        response.setDateAdded(order.getDateAdded());
        response.setDepartment(order.getDepartment());
        
        if (order.getCustomers() != null && !order.getCustomers().isEmpty()) {
            List<CustomerInfo> customerInfos = order.getCustomers().stream()
                .map(customer -> {
                    CustomerInfo info = new CustomerInfo();
                    info.setCustomerId(customer.getCustomerId());
                    info.setCustomerName(customer.getCustomerName());
                    info.setCompanyName(customer.getCompanyName());
                    info.setCustomerEmail(customer.getCustomerEmail());
                    info.setCustomerPhone(customer.getCustomerPhone());
                    info.setPrimaryAddress(customer.getPrimaryAddress());
                    info.setBillingAddress(customer.getBillingAddress());
                    info.setShippingAddress(customer.getShippingAddress());
                    return info;
                })
                .collect(Collectors.toList());
            response.setCustomers(customerInfos);
        }
        
        if (order.getProducts() != null && !order.getProducts().isEmpty()) {
            List<ProductInfo> productInfos = order.getProducts().stream()
                .map(product -> {
                    ProductInfo info = new ProductInfo();
                    info.setProductId(product.getId());
                    info.setProductCode(product.getProductCode());
                    info.setProductName(product.getProductName());
                    return info;
                })
                .collect(Collectors.toList());
            response.setProducts(productInfos);
        }
        
        return response;
    }
}
