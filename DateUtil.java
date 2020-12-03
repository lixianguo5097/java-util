package com.hqx.util;


import com.alibaba.fastjson.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 时间工具类
 * @author LXG
 * @date 2019-11-5
 */
public class DateUtil {
    /**
     * 一天的时间戳
     */
    public static int ONE_DAY_TIMESTAMPS = 60 * 60 * 24 * 1000;
    /**
     * 加八小时的时间戳
     */
    public static int ADD_EIGHT_HOURS_TIMESTAMPS = 60 * 60 * 8 * 1000;

    /**
     * 获取当天零点的时间
     */
    public static Date getZeroOClock() {
        Long currentTimestamps = System.currentTimeMillis();
        Long oneDayTimestamps = Long.valueOf(ONE_DAY_TIMESTAMPS);
        Date date = new Date();
        date.setTime(currentTimestamps - (currentTimestamps + ADD_EIGHT_HOURS_TIMESTAMPS) % oneDayTimestamps);
        return date;
    }

    /**
     * 获取当前时间的整点
     */
    public static String getCurrentHour() {
        return formatDate(new Date(),4);
    }

    /**
     * 获取一天中所有的整点时间
     * @param reqDate
     * @return
     */
    public static List<String> getAllHourClock(String reqDate)  {
        Date date = formatDate(reqDate + " 00:00:00",4);
        List<String> dates = new ArrayList<>();
        int size = 24;
        for (int i = 0; i < size; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            if (i != 0) {
                cal.add(Calendar.HOUR, 1);
            }
            date = cal.getTime();
            dates.add(DateUtil.formatDate(date,0));
        }
        return dates;
    }

    /**
     * 获取两个日期之间的天数
     */
    public static long getDateSpace(String beginDate, String endDate){
        //得到相差的天数 betweenDate
        Date endDateTime = formatDate(endDate, 1);
        Date beginDateTime = formatDate(beginDate, 1);
        long betweenDate = (endDateTime.getTime() - beginDateTime.getTime()) / (60 * 60 * 24 * 1000);
        return betweenDate;
    }

    /**
     * 获取当年月有几天
     *
     * @param date
     * @return list中第一个参数是当月有几天，第二个是当年有几天
     * @throws ParseException
     */
    public static List<Integer> getDaysCount(String date){
        ArrayList<Integer> list = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formatDate(date,2));
        list.add(calendar.getActualMaximum(5));
        list.add(calendar.getActualMaximum(6));
        return list;
    }

    /**
     * 获取当前周的周一和周日
     *
     * @param date
     * @return
     */
    public static List<Date> getWeekByDate(Date date){
        //格式化日期
        List<Date> dateList = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        // 判断要计算的日期是否是周日，如果是则减一天计算周六的，否则会出问题，计算到下一周去了
        // 获得当前日期是一个星期的第几天
        int dayWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (1 == dayWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1);
        }
        // 设置一个星期的第一天，按中国的习惯一个星期的第一天是星期一
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        // 获得当前日期是一个星期的第几天
        int day = cal.get(Calendar.DAY_OF_WEEK);
        // 根据日历的规则，给当前日期减去星期几与一个星期第一天的差值
        cal.add(Calendar.DATE, cal.getFirstDayOfWeek() - day);
        Date date1 = formatDate(cal.getTime(), 2);
        dateList.add(formatDate(date1,2));
        Date date2 = formatDate(cal.getTime(), 2);
        dateList.add(formatDate(date2,2));
        return dateList;
    }

    /**
     * 获得两个日期之间的所有日期
     *
     * @param beginDate 开始时间
     * @param endDate 结束实际按
     * @return
     */
    public static List<String> getBetweenDates(String beginDate, String endDate){
        //定义起始日期
        Date start = formatDate(beginDate, 1);
        //定义结束日期
        Date end = formatDate(endDate, 1);

        List<String> result = new ArrayList<String>();
        Calendar tempStart = Calendar.getInstance();
        tempStart.setTime(start);

        Calendar tempEnd = Calendar.getInstance();
        tempEnd.setTime(end);
        while (tempStart.before(tempEnd) || tempStart.equals(tempEnd)) {
            result.add(formatDate(tempStart.getTime(), 1));
            tempStart.add(Calendar.DAY_OF_YEAR, 1);
        }
        return result;
    }

    /**
     * 获取某段时间之间的月份
     * @return 月份列表
     */
    public static List<String> getMonthBetween(JSONObject reqJson)  {
        List<String> result = new ArrayList<>();
        //定义起始日期
        Date d1 = formatDate(reqJson.getString( "beginDate" ),1);
        //定义结束日期
        Date d2 = formatDate(reqJson.getString( "endDate" ),1);
        //定义日期实例
        Calendar dd = Calendar.getInstance();
        //设置日期起始时间
        dd.setTime(d1);
        //判断是否到结束日期
        while(dd.getTime().before(d2)){
            String str = formatDate(dd.getTime(), 2);
            result.add(str);
            dd.add( Calendar.MONTH, 1);
        }
        result.add( formatDate(d2,2));
        return result;
    }

    /**
     * 获取两个日期之间的所有日期
     * @return 日期列表
     */
    public static List<String> getBetweenDates(JSONObject reqJson)   {
        //定义起始日期
        Date start = formatDate(reqJson.getString( "beginDate" ),1);
        //定义结束日期
        Date end = formatDate(reqJson.getString( "endDate" ),1);

        List<String> result = new ArrayList<>();
        Calendar tempStart = Calendar.getInstance();
        tempStart.setTime(start);

        Calendar tempEnd = Calendar.getInstance();
        tempEnd.setTime(end);
        while (tempStart.before(tempEnd) || tempStart.equals(tempEnd)) {
            result.add(formatDate(tempStart.getTime(),1));
            tempStart.add(Calendar.DAY_OF_YEAR, 1);
        }
        return result;
    }

    /**
     * 格式化日期
     */
    public static <T> T formatDate(Object o, Integer patternNo) {
        String pattern = null;
        if (patternNo == 0) {
            pattern = "yyyy-MM-dd HH:mm:ss";
        } else if (patternNo == 1) {
            pattern = "yyyy-MM-dd";
        } else if (patternNo == 2) {
            pattern = "yyyy-MM";
        } else if (patternNo == 3) {
            pattern = "yyyy";
        } else if (patternNo==4) {
            pattern = "yyyy-MM-dd HH:00:00";
        } else if (patternNo == 5) {
            pattern = "HH";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        try {
            if (o instanceof String) {
                return (T) sdf.parse((String) o);

            }
            if (o instanceof Date) {
                return (T) sdf.format((Date) o);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

}
