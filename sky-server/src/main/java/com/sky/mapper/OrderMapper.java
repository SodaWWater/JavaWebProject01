package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 新增用户订单信息
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 查询历史订单
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 查询订单详细
     * @return
     */
    @Select("select * from sky_take_out.orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 更新订单状态
     * @param order
     */
    void update(Orders order);

    /**
     * 根据状态统计订单数量
     * @param status
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    @Select("select  * from sky_take_out.orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusOrderTimeLT(Integer status, LocalDateTime orderTime);
}
