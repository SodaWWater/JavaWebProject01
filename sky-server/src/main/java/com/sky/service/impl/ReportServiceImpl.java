package com.sky.service.impl;


import com.alibaba.fastjson.JSON;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
//        当前集合用于存放从begin到end范围内的每天日期
        List<LocalDate> dateList = new ArrayList<>();
            dateList.add(begin);
        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
//        存放每天营业额
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
//            查询date对应的营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
            map.put("begin",beginTime);
            map.put("end",endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
//            如果查询不到数据 会返null 所以当为空时 将营业额写为0
            turnover = turnover == null ? 0.0 : turnover ;
            turnoverList.add(turnover);
        }
//      封装返回结果
        String date = StringUtils.join(dateList, ",");
        String turnover = StringUtils.join(turnoverList, ",");
        return TurnoverReportVO.builder()
                .dateList(date)
                .turnoverList(turnover)
                .build();
    }

    /**
     * 用户人数统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
//        存放每天新增用户数量
        List<Integer> newUserList = new ArrayList<>();
//        存放当天总用户量
        List<Integer> totalUserList = new ArrayList<>();
//        遍历每天
        for (LocalDate date : dateList) {
//            先查总数
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            Map<String,LocalDateTime> map = new HashMap<>();

            map.put("end",endTime);
            Integer total = userMapper.countByMap(map);
            map.put("begin",beginTime);
            Integer singleDay = userMapper.countByMap(map);

            newUserList.add(singleDay);
            totalUserList.add(total);
        }


        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }


    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> dailyList = new ArrayList();
        List<Integer> totalList = new ArrayList();
//        查询每天订单数
        for (LocalDate date : dateList) {
            Map map = new HashMap<>();
//            每日订单总数
            LocalDateTime begin1 = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end1 = LocalDateTime.of(date, LocalTime.MAX);
            Integer total = getOrderCount(begin1,end1,null);
//            有效订单数
            Integer daily = getOrderCount(begin1,end1,Orders.COMPLETED);
            dailyList.add(daily);
            totalList.add(total);
        }
//        计算订单总数
        Integer dailyTotals = dailyList.stream().reduce(Integer::sum).get();
        Integer allTotals = totalList.stream().reduce(Integer::sum).get();
//        完成率
        Double orderCompletionRate = 0.0;
        if (allTotals != 0) {
            orderCompletionRate =  dailyTotals.doubleValue()/allTotals * 100;
        }

        return  OrderReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .orderCountList(StringUtils.join(totalList,","))
                .validOrderCountList(StringUtils.join(dailyList,","))
                .totalOrderCount(allTotals)
                .validOrderCount(dailyTotals)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 根据条件统计订单数量
     * @param begin
     * @param end
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime begin,LocalDateTime end,Integer status){
        Map map = new HashMap<>();
        map.put("begin",begin);
        map.put("end",end);
        map.put("status",status);

        return  orderMapper.countByMap(map);
    }
}
