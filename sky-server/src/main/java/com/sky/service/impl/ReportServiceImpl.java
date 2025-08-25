package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.entity.User;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ReportMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
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
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();// 营业额集合
        for (LocalDate date : dateList){
            // 获取营业额数据

            // 获取指定日期的营业额
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnoverList.add(turnover != null ? turnover : 0.0);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }


    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map = new HashMap();
            map.put("end", endTime);

            totalUserList.add(userMapper.countByMap(map));

            map.put("begin", beginTime);
            newUserList.add(userMapper.countByMap(map));

        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();

    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }


        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList){

            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // 查询订单总数
            Integer orderCount = orderMapper.getOrderCount(beginTime, endTime, null);
            // 查询有效订单数
            Integer validOrderCount = orderMapper.getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        // 计算时间区间内的订单总数量
        Integer totalOrderCount = orderCountList.stream().reduce(0, Integer::sum);
        // 计算时间区间内的有效订单数量
        Integer validOrderCount = validOrderCountList.stream().reduce(0, Integer::sum);

        // 计算订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount.doubleValue();
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status){
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {

        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(beginTime, endTime);
        List<String> names = salesTop10.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        String nameList = StringUtils.join(names, ",");
        List<Integer> numbers = salesTop10.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());
        String numberList = StringUtils.join(numbers, ",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build()
                ;
    }

    @Override
    public void exportBusinessData(HttpServletResponse  response) {
        // 查询数据库 获取营业数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        LocalDateTime begin = LocalDateTime.of(dateBegin, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(dateEnd, LocalTime.MAX);
        BusinessDataVO businessData = workspaceService.getBusinessData(begin, end);
        // 通过POI将数据写入excel文件
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try{
            // 创建工作簿
            XSSFWorkbook excel = new XSSFWorkbook(in);
            // 获取订单表格 sheet 页
            XSSFSheet sheet = excel.getSheet("Sheet1");
            // 填充数据
            // 填充时间
            sheet.getRow(1).getCell(1).setCellValue("时间：" + dateBegin + "至" + dateEnd);


            XSSFRow row = sheet.getRow(3);
            // 填充营业额
            row.getCell(2).setCellValue(businessData.getTurnover());
            // 填充订单完成率
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            // 填充新用户数
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            // 填充有效订单数
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            // 填充平均客单价
            row.getCell(4).setCellValue(businessData.getUnitPrice());


            // 填充明细数据
            for (int i = 0 ; i < 30 ; i ++){
                LocalDate date = dateBegin.plusDays(i);
                LocalDateTime begin2 = LocalDateTime.of(date, LocalTime.MIN);
                LocalDateTime end2 = LocalDateTime.of(date, LocalTime.MAX);
                BusinessDataVO businessData1 = workspaceService.getBusinessData(begin2, end2);
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(businessData1.getTurnover());
                row.getCell(3).setCellValue(businessData1.getValidOrderCount());
                row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());
                row.getCell(5).setCellValue(businessData1.getUnitPrice());
                row.getCell(6).setCellValue(businessData1.getNewUsers());
            }

            // 通过输出流下载到客户端浏览器
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭流
            out.close();
            excel.close();

        }catch (IOException e){
            e.printStackTrace();
        }





    }
}
