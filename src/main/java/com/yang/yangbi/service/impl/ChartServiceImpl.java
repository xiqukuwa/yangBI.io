package com.yang.yangbi.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.yang.yangbi.constant.CommonConstant;
import com.yang.yangbi.model.dto.chart.ChartQueryRequest;
import com.yang.yangbi.model.entity.Chart;
import com.yang.yangbi.service.ChartService;
import com.yang.yangbi.mapper.ChartMapper;
import com.yang.yangbi.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
* @author 86176
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-08-01 09:47:04
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{


    @Override
//    @CacheEvict(value = "chart", allEntries = true)
    public boolean updateById(Chart entity) {
        return SqlHelper.retBool(this.getBaseMapper().updateById(entity));
    }

    @Override
//    @CacheEvict(value = "chart", allEntries = true)
    public boolean save(Chart entity) {
        return SqlHelper.retBool(this.getBaseMapper().insert(entity));
    }


    @Override
//    @Cacheable( value= "chart",key="#chartQueryRequest")
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        System.out.println("缓存了");
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }
}




