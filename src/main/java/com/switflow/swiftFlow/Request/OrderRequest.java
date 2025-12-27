package com.switflow.swiftFlow.Request;

import com.switflow.swiftFlow.utility.Department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    

    private String productDetails;

    private String customProductDetails;

    private String units;

    private String material;

    private Department department;

    private Integer customerId;

    private Integer productId;

}
