package com.yang.yangbi.mapper;

import com.yang.yangbi.config.MybatisRedisCache;
import com.yang.yangbi.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.CacheNamespace;

/**
* @author 86176
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2023-08-01 09:47:04
* @Entity com.yang.yangbi.model.entity.Chart
*/

//@CacheNamespace(implementation= MybatisRedisCache.class,eviction=MybatisRedisCache.class)
public interface ChartMapper extends BaseMapper<Chart> {

}




