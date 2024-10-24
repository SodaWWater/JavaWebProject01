package com.sky.service.impl;


import com.alibaba.fastjson.JSON;
import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

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
            orderCompletionRate =  dailyTotals.doubleValue()/allTotals;
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
     * 销量排名
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSaleTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop = orderMapper.getSalesTop(beginTime, endTime);
        List<String> names = salesTop.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameSting = StringUtils.join(names, ",");
        List<Integer> number = salesTop.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberString = StringUtils.join(number, ",");
        return  SalesTop10ReportVO.builder()
                .nameList(nameSting)
                .numberList(numberString)
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

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库获取数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);

        BusinessDataVO businessDataVO = workspaceService.getBusinessData(begin, end);
        //2.通过POI将数据写入Excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            //基于模板创建文件
            XSSFWorkbook excel = new XSSFWorkbook(in);
            //填充数据--时间
            XSSFSheet sheet1 = excel.getSheet("sheet1");

            sheet1.getRow(1).getCell(1).setCellValue("时间:"+dateBegin+"至" +dateEnd);

            XSSFRow row = sheet1.getRow(3);
            row.getCell(2).setCellValue(businessDataVO.getTurnover());
            row.getCell(4).setCellValue(businessDataVO.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessDataVO.getNewUsers());

//            获取第五行
            XSSFRow row5 = sheet1.getRow(4);
            row5.getCell(2).setCellValue(businessDataVO.getValidOrderCount());
            row5.getCell(4).setCellValue(businessDataVO.getUnitPrice());
//             填充明细数据
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
//                查询某一天的数据
                BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
//                获得某一行
                row = sheet1.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData.getTurnover());
                row.getCell(3).setCellValue(businessData.getValidOrderCount());
                row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData.getUnitPrice());
                row.getCell(6).setCellValue(businessData.getNewUsers());

            }

            //3.通过输出流将Excel文件下载到客户端
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            out.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
