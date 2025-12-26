package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.Orders;
import com.switflow.swiftFlow.Entity.Status;
import com.switflow.swiftFlow.Repo.OrderRepository;
import com.switflow.swiftFlow.Repo.StatusRepository;
import com.switflow.swiftFlow.Request.StatusRequest;
import com.switflow.swiftFlow.Response.StatusResponse;
import com.switflow.swiftFlow.Exception.OrderNotFoundException;
import com.switflow.swiftFlow.utility.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatusService {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    public StatusResponse createStatus(StatusRequest statusRequest, long orderId) {
        Orders order = this.orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        Status status = new Status();
        // Set old status from the order's current department
        status.setOldStatus(order.getDepartment());
        // Set new status from the request
        status.setNewStatus(statusRequest.getNewStatus());
        status.setComment(statusRequest.getComment());
        status.setPercentage(statusRequest.getPercentage());
        
        // Set attachment URL from the request (this could be a direct URL or one obtained from Cloudinary)
        status.setAttachmentUrl(statusRequest.getAttachmentUrl());
        status.setOrders(order);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        status.setCreatedAt(LocalDate.now().format(formatter));

        Status savedStatus = statusRepository.save(status);

        // Update the order's department to the new status
        order.setDepartment(statusRequest.getNewStatus());
        orderRepository.save(order);

        StatusResponse response = new StatusResponse();
        response.setId(savedStatus.getId());
        response.setNewStatus(savedStatus.getNewStatus());
        response.setOldStatus(savedStatus.getOldStatus());
        response.setComment(savedStatus.getComment());
        // Return the attachment URL that was saved to the database
        response.setAttachmentUrl(savedStatus.getAttachmentUrl());
        response.setOrderId(savedStatus.getOrders().getOrderId());
        response.setPercentage(savedStatus.getPercentage());
        response.setCreatedAt(savedStatus.getCreatedAt());

        return response;
    }

    public StatusResponse createFilteredPdfStatus(long orderId, String filteredPdfUrl, Department targetDepartment) {
        Orders order = this.orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));

        Status status = new Status();
        status.setOldStatus(order.getDepartment());
        status.setNewStatus(targetDepartment);
        status.setComment("Filtered PDF generated for " + targetDepartment.name());
        status.setPercentage(null);
        status.setAttachmentUrl(filteredPdfUrl);
        status.setOrders(order);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        status.setCreatedAt(LocalDate.now().format(formatter));

        Status savedStatus = statusRepository.save(status);

        order.setDepartment(targetDepartment);
        orderRepository.save(order);

        StatusResponse response = new StatusResponse();
        response.setId(savedStatus.getId());
        response.setNewStatus(savedStatus.getNewStatus());
        response.setOldStatus(savedStatus.getOldStatus());
        response.setComment(savedStatus.getComment());
        response.setAttachmentUrl(savedStatus.getAttachmentUrl());
        response.setOrderId(savedStatus.getOrders().getOrderId());
        response.setPercentage(savedStatus.getPercentage());
        response.setCreatedAt(savedStatus.getCreatedAt());

        return response;
    }

    public StatusResponse createStatus(StatusRequest statusRequest, long orderId, MultipartFile attachment) throws IOException {
        // Upload attachment to Cloudinary if provided
        if (attachment != null && !attachment.isEmpty()) {
            String attachmentUrl = cloudinaryService.uploadFile(attachment);
            statusRequest.setAttachmentUrl(attachmentUrl);
        }
        
        // Call the main createStatus method with the updated statusRequest
        return createStatus(statusRequest, orderId);
    }
    
    public String uploadAttachment(MultipartFile attachment) throws IOException {
        if (attachment == null || attachment.isEmpty()) {
            throw new IllegalArgumentException("Attachment file is empty");
        }
        return cloudinaryService.uploadFile(attachment);
    }
   
    public List<StatusResponse> getStatusesByOrderId(long orderId) {
        return statusRepository.findByOrdersOrderId(orderId).stream()
                .map(status -> {
                    StatusResponse response = new StatusResponse();
                    response.setId(status.getId());
                    response.setNewStatus(status.getNewStatus());
                    response.setOldStatus(status.getOldStatus());
                    response.setComment(status.getComment());
                    response.setAttachmentUrl(status.getAttachmentUrl());
                    response.setOrderId(status.getOrders().getOrderId());
                    response.setPercentage(status.getPercentage());
                    response.setCreatedAt(status.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }
}