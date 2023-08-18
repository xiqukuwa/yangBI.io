package com.yang.yangbi.model.vo;

import lombok.Data;

/**
 * bi的返回结果
 */
@Data
public class BiResponse {

    private String genChart;
    private String genResult;
    private Long chartId;
    private String status;
}
