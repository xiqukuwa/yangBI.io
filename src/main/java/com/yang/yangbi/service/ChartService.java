package com.yang.yangbi.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yang.yangbi.model.dto.chart.ChartQueryRequest;
import com.yang.yangbi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.poi.ss.formula.functions.T;

/**
* @author 86176
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2023-08-01 09:47:04
*/
public interface ChartService extends IService<Chart> {

     boolean updateById(Chart entity);

     boolean save(Chart entity);

     QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

}
