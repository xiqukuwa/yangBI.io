package com.yang.yangbi.manager;

import com.yang.yangbi.common.ErrorCode;
import com.yang.yangbi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    public void doRateLimit(String key){
        //创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL,6,3, RateIntervalUnit.SECONDS);
        boolean result = rateLimiter.tryAcquire(1);
        if (!result){
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
