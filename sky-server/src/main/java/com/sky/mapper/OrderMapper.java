package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper {

    /**
     * 新增用户订单信息
     * @param orders
     */
    void insert(Orders orders);
}
