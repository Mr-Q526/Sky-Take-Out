package com.sky.service.impl;


import com.sky.mapper.ShopMapper;
import com.sky.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Integer getStatus() {
        String value = (String)redisTemplate.opsForValue().get("SHOP_STATUS");
        return Integer.valueOf(value);
    }

    @Override
    public void updateStatus(Integer status) {
        //设置Redis中的店铺营业状态
        redisTemplate.opsForValue().set("SHOP_STATUS", String.valueOf(status));
    }
}
