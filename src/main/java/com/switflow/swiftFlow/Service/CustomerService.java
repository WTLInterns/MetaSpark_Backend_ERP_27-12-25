package com.switflow.swiftFlow.Service;

import com.switflow.swiftFlow.Entity.Customer;
import com.switflow.swiftFlow.Exception.CustomerEmailAlreadyExistsException;
import com.switflow.swiftFlow.Repo.CustomerRepo;
import com.switflow.swiftFlow.Request.CustomerRequest;
import com.switflow.swiftFlow.Response.CustomerResposne;
import com.switflow.swiftFlow.Response.MessageResponse;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepo customerRepo;

    public CustomerResposne createCustomer(CustomerRequest customerRequest) {
        if (customerRequest.getCustomerName() == null || customerRequest.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("Customer name is required");
        }

        if (customerRequest.getCustomerEmail() == null || customerRequest.getCustomerEmail().trim().isEmpty()) {
            throw new RuntimeException("Customer email is required");
        }

        if (customerRepo.findByCustomerEmail(customerRequest.getCustomerEmail()).isPresent()) {
            throw new CustomerEmailAlreadyExistsException("Customer with this email already exists");
        }

        try {
            Customer customer = new Customer();
            customer.setCustomerName(customerRequest.getCustomerName());
            customer.setCompanyName(customerRequest.getCompanyName());
            customer.setCustomerEmail(customerRequest.getCustomerEmail());
            customer.setCustomerPhone(customerRequest.getCustomerPhone());
            customer.setGstNumber(customerRequest.getGstNumber());
            customer.setPrimaryAddress(customerRequest.getPrimaryAddress());
            customer.setStatus("Active");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            customer.setDateAdded(LocalDate.now().format(formatter));

            Customer savedCustomer = customerRepo.save(customer);

            CustomerResposne response = new CustomerResposne();
            response.setCustomerId(savedCustomer.getCustomerId());
            response.setCustomerName(savedCustomer.getCustomerName());
            response.setCompanyName(savedCustomer.getCompanyName());
            response.setCustomerEmail(savedCustomer.getCustomerEmail());
            response.setCustomerPhone(savedCustomer.getCustomerPhone());
            response.setGstNumber(savedCustomer.getGstNumber());
            response.setPrimaryAddress(savedCustomer.getPrimaryAddress());
            response.setStatus(savedCustomer.getStatus());
            response.setDateAdded(savedCustomer.getDateAdded());
            response.setBillingAddress(savedCustomer.getBillingAddress());
            response.setShippingAddress(savedCustomer.getShippingAddress());

            return response;
        } catch (CustomerEmailAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error creating customer: " + e.getMessage());
            throw new RuntimeException("Failed to create customer: " + e.getMessage());
        }
    }

    public List<CustomerResposne> getAllCustomers() {
        return customerRepo.findAll().stream().map(customer -> {
            CustomerResposne response = new CustomerResposne();
            response.setCustomerId(customer.getCustomerId());
            response.setCustomerName(customer.getCustomerName());
            response.setCompanyName(customer.getCompanyName());
            response.setCustomerEmail(customer.getCustomerEmail());
            response.setCustomerPhone(customer.getCustomerPhone());
            response.setGstNumber(customer.getGstNumber());
            response.setPrimaryAddress(customer.getPrimaryAddress());
            response.setStatus(customer.getStatus());
            response.setDateAdded(customer.getDateAdded());
            return response;
        }).collect(Collectors.toList());
    }

    public CustomerResposne getCustomerById(int customerId) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        CustomerResposne response = new CustomerResposne();
        response.setCustomerId(customer.getCustomerId());
        response.setCustomerName(customer.getCustomerName());
        response.setCompanyName(customer.getCompanyName());
        response.setCustomerEmail(customer.getCustomerEmail());
        response.setCustomerPhone(customer.getCustomerPhone());
        response.setGstNumber(customer.getGstNumber());
        response.setPrimaryAddress(customer.getPrimaryAddress());
        response.setStatus(customer.getStatus());
        response.setDateAdded(customer.getDateAdded());
        response.setBillingAddress(customer.getBillingAddress());
        response.setShippingAddress(customer.getShippingAddress());
        return response;
    }

    public CustomerResposne updateCustomer(int customerId, CustomerRequest customerRequest) {
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        customer.setCustomerName(customerRequest.getCustomerName());
        customer.setCompanyName(customerRequest.getCompanyName());
        customer.setCustomerEmail(customerRequest.getCustomerEmail());
        customer.setCustomerPhone(customerRequest.getCustomerPhone());
        customer.setGstNumber(customerRequest.getGstNumber());
        customer.setPrimaryAddress(customerRequest.getPrimaryAddress());
        customer.setStatus(customerRequest.getStatus());
        customer.setBillingAddress(customerRequest.getBillingAddress());
        customer.setShippingAddress(customerRequest.getShippingAddress());
        Customer updatedCustomer = customerRepo.save(customer);

        CustomerResposne response = new CustomerResposne();
        response.setCustomerId(updatedCustomer.getCustomerId());
        response.setCustomerName(updatedCustomer.getCustomerName());
        response.setCompanyName(updatedCustomer.getCompanyName());
        response.setCustomerEmail(updatedCustomer.getCustomerEmail());
        response.setCustomerPhone(updatedCustomer.getCustomerPhone());
        response.setGstNumber(updatedCustomer.getGstNumber());
        response.setPrimaryAddress(updatedCustomer.getPrimaryAddress());
        response.setStatus(updatedCustomer.getStatus());
        response.setBillingAddress(updatedCustomer.getBillingAddress());
        response.setShippingAddress(updatedCustomer.getShippingAddress());
        response.setDateAdded(updatedCustomer.getDateAdded());
        return response;

    }

    public MessageResponse deleteCustomer(int customerId) {
        customerRepo.deleteById(customerId);
        return new MessageResponse("Customer deleted successfully");
    }

    public MessageResponse importCustomersFromExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("No file uploaded");
        }

        int createdCount = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new RuntimeException("Uploaded file has no sheets");
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new RuntimeException("Uploaded file is empty");
            }

            Row headerRow = rowIterator.next();
            int nameCol = -1;
            int companyCol = -1;
            int phoneCol = -1;
            int gstCol = -1;

            for (Cell cell : headerRow) {
                String header = cell.getStringCellValue();
                if (header == null) continue;
                String h = header.trim().toLowerCase(Locale.ROOT);
                if (h.equals("name")) nameCol = cell.getColumnIndex();
                else if (h.equals("company")) companyCol = cell.getColumnIndex();
                else if (h.equals("phone") || h.equals("phone no") || h.equals("phone number")) phoneCol = cell.getColumnIndex();
                else if (h.equals("gst no") || h.equals("gst") || h.equals("gst number")) gstCol = cell.getColumnIndex();
            }

            if (nameCol == -1) {
                throw new RuntimeException("Header 'Name' column is required");
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    String name = getCellString(row.getCell(nameCol));
                    if (name == null || name.isBlank()) {
                        continue; // skip empty rows
                    }
                    String company = companyCol >= 0 ? getCellString(row.getCell(companyCol)) : null;
                    String phone = phoneCol >= 0 ? getCellString(row.getCell(phoneCol)) : null;
                    String gst = gstCol >= 0 ? getCellString(row.getCell(gstCol)) : null;

                    Customer customer = new Customer();
                    customer.setCustomerName(name.trim());
                    customer.setCompanyName(company != null ? company.trim() : null);
                    customer.setCustomerPhone(phone != null ? phone.trim() : null);
                    customer.setGstNumber(gst != null ? gst.trim() : null);
                    customer.setStatus("Active");
                    customer.setDateAdded(LocalDate.now().format(formatter));

                    Customer saved = customerRepo.save(customer);
                    if (saved != null && saved.getCustomerId() > 0) {
                        createdCount++;
                    }
                } catch (Exception rowEx) {
                    errors.add("Failed to import row " + row.getRowNum() + ": " + rowEx.getMessage());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import customers: " + e.getMessage());
        }

        StringBuilder sb = new StringBuilder("Imported ").append(createdCount).append(" customers");
        if (!errors.isEmpty()) {
            sb.append(" with ").append(errors.size()).append(" row errors");
        }
        return new MessageResponse(sb.toString());
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}