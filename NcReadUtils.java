package com.hqx.common.netcdf.utils;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import com.hqx.common.core.exception.MyException;
import com.hqx.common.netcdf.req.RiverWaterAndTideChartListReq;
import com.hqx.common.netcdf.resp.*;
import lombok.extern.slf4j.Slf4j;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GlobalCoordinates;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * NC读取工具类
 *
 * @author 20170029
 */
@Slf4j
public class NcReadUtils {


//    public static void main(String[] args) {
//        RiverWaterAndTideChartListReq riverWaterAndTideChartListReq = new RiverWaterAndTideChartListReq();
//        riverWaterAndTideChartListReq.setPath("http://192.168.2.13/opendap/storm_forecast_zeta.nc");
//        riverWaterAndTideChartListReq.setLatPoint(17.81132F);
//        riverWaterAndTideChartListReq.setLonPoint(125.59473F);
//        riverWaterAndTideChartListReq.setStartTime(1597716000000L);
//        riverWaterAndTideChartListReq.setEndTime(1597971600000L);
//        riverWaterAndTideChartListReq.setNeedDykeLevel(false);
//        RiverWaterCResultResp stormLevelChart = getStormLevelChart(riverWaterAndTideChartListReq);
//    }

    public static RiverWaterCTimeResultResp getStormLevelTime(String path) {
        NetcdfFile openNC = null;
        try {

            openNC = getNetcdfFile(path);

            //通过时间获取列数
            //获取时间列表，得到所需时间下标数组
            //time
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();

            //获取time
            int startTime = timeValue[0];
            int endTime = timeValue[timeValue.length - 1];

            RiverWaterCTimeResultResp riverWaterCTimeResultResp = new RiverWaterCTimeResultResp();
            riverWaterCTimeResultResp.setStartTime(new Date(startTime * 1000L));
            riverWaterCTimeResultResp.setEndTime(new Date(endTime * 1000L));

            return riverWaterCTimeResultResp;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (openNC != null) {
                try {
                    openNC.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    /**
     * 获取某个经纬度的折线图
     *
     * @param riverWaterAndTideChartListReq
     * @return
     */
    public static RiverWaterCResultResp getStormLevelChart(RiverWaterAndTideChartListReq
                                                                   riverWaterAndTideChartListReq) {
        NetcdfFile openNC = null;
        try {
            RiverWaterCResultResp resultResp = new RiverWaterCResultResp();

            openNC = NetcdfFile.openInMemory(new URI(riverWaterAndTideChartListReq.getPath()));
            //纬度NLatitude
            Variable latitudeVar = openNC.findVariable("NLatitude");
            int[] latitudeValue = new int[0];
            latitudeValue = (int[]) latitudeVar.read().copyTo1DJavaArray();
            //经度NLongitude
            Variable longitudeVar = openNC.findVariable("NLongitude");
            int[] longitudeValue = new int[0];
            longitudeValue = (int[]) longitudeVar.read().copyTo1DJavaArray();

            //通过时间获取列数
            //获取时间列表，得到所需时间下标数组
            //time
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();

            //获取time下标
            List<Integer> timeIndex = new ArrayList<>();
            int intStartTime = (int) (riverWaterAndTideChartListReq.getStartTime() / 1000L);
            int intEndTime = (int) (riverWaterAndTideChartListReq.getEndTime() / 1000L);

            List<Long> time = new ArrayList<>();
            for (int i = 0; i < timeValue.length; i++) {
                if (timeValue[i] <= intEndTime && timeValue[i] >= intStartTime) {
                    timeIndex.add(i);
                    //时间数组填充
                    time.add(timeValue[i] * 1000L);
                }
            }

            if (time.size() == 0) {
//                throw MyException.newException("该时间范围不存在内容");
                return null;
            }
            resultResp.setTime(time);

            //将经度的函数放入set
            List<Integer> list = new ArrayList<>();

            //1、通过经度获取行数
            for (int i = 0; i < longitudeValue.length; i++) {
                if ((Float.valueOf(longitudeValue[i]) / 100000f >= riverWaterAndTideChartListReq.getLonPoint() - riverWaterAndTideChartListReq.getErrorRange())
                        && (Float.valueOf(longitudeValue[i]) / 100000f <= riverWaterAndTideChartListReq.getLonPoint() + riverWaterAndTideChartListReq.getErrorRange())) {
                    list.add(i);
                }
            }

            //2、去经度获取对应的数据
            List<Integer> collect = new ArrayList<>();
            if (!list.isEmpty()) {
                int[] finalLatitudeValue = latitudeValue;
                collect = list.stream().filter(lonLocation -> {
                    int lat = finalLatitudeValue[lonLocation];
                    if ((Float.valueOf(lat) / 100000f - riverWaterAndTideChartListReq.getErrorRange() <= riverWaterAndTideChartListReq.getLatPoint()) && (Float.valueOf(lat) / 100000f + riverWaterAndTideChartListReq.getErrorRange() >= riverWaterAndTideChartListReq.getLatPoint())) {
                        return true;
                    }
                    return false;
                }).collect(Collectors.toList());
            } else {
                //返回内容设置为空数组
                resultResp.setLevel(new ArrayList<>());
                return resultResp;
            }

            //经纬度的下标值
            Integer index = null;

            if (collect.size() > 1) {
                Double meter = null;
                //说明有多个匹配点，需要再次挑选
                //找到最贴近的点，比对和请求经纬度的距离，取到最近的一个点
                for (Integer point : collect) {
                    int lat = latitudeValue[point];
                    int lon = longitudeValue[point];
                    GlobalCoordinates source = new GlobalCoordinates(riverWaterAndTideChartListReq.getLatPoint(), riverWaterAndTideChartListReq.getLonPoint());
                    GlobalCoordinates target = new GlobalCoordinates(lat, lon);
                    double distance = CaculateDistance.getDistanceMeter(source, target, Ellipsoid.Sphere);
                    if (meter == null || distance < meter) {
                        meter = distance;
                        index = point;
                    }
                }
            } else {
                index = list.get(0);
            }

            //ssh
            Variable sshVar = openNC.findVariable("ssh");
            int[] sshValue = new int[0];
            sshValue = (int[]) sshVar.read().copyTo1DJavaArray();

            //通过经纬度计算到的行数，获取到整行的值
            //3、水位预警结束
            Dimension dimension = sshVar.getDimension(1);

            //水位值
            List<Double> level = new ArrayList<>(time.size());

            for (Integer i : timeIndex) {
                int ssh = sshValue[i * dimension.getLength() + index];
                level.add(((double) ssh) / 100000);
            }
            resultResp.setLevel(level);

            //坝dykeHeig
            if (riverWaterAndTideChartListReq.getNeedDykeLevel()) {
                Variable dykeHeigVar = openNC.findVariable("dyke_heig");
                int[] dykeHigh = new int[0];
                dykeHigh = (int[]) dykeHeigVar.read().copyTo1DJavaArray();
                if (index <= dykeHigh.length) {
                    resultResp.setDykeLevel(((double) (dykeHigh[index]) / 100000));
                }
            }
            return resultResp;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw MyException.newException("风暴潮折线图读取异常");
        } catch (IOException e) {
            e.printStackTrace();
            throw MyException.newException("风暴潮折线图读取异常");
        } finally {
            if (openNC != null) {
                try {
                    openNC.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 根据本地路径或网络路径读取nc文件
     *
     * @param urlOrLocation
     * @return
     * @throws Exception
     */
    public static NetcdfFile getNetcdfFile(String urlOrLocation) {

        boolean isUrl = urlOrLocation.startsWith("http");

        try {
            if (isUrl) {
                return NetcdfFile.openInMemory(new URI(urlOrLocation));
            } else {
                boolean exist = FileUtil.exist(urlOrLocation);
                if (!exist) {
                    log.error("------文件不存在:{}", urlOrLocation);
                    throw MyException.newException("文件:" + urlOrLocation + "不存在");
                }
                return NetcdfFile.openInMemory(urlOrLocation);
            }
        } catch (MyException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取nc文件" + urlOrLocation + "失败", e);
            throw MyException.newException("读取nc文件失败");
        }

    }

    /**
     * 获取站点预测潮位/预测水位预测数据
     * station_tide_forecast.nc
     *
     * @param urlOrLocation 网络路径或本地路径
     * @return
     */
    public static List<StationPredictNcParseResp> getStationTideOrWaterLevelPredict(String urlOrLocation) {

        NetcdfFile openNC = null;
        try {
            openNC = getNetcdfFile(urlOrLocation);
            //站点变量
            Variable stationNameVar = openNC.findVariable("station_name");
            int[] stationNumber = (int[]) stationNameVar.read().copyTo1DJavaArray();

            //时间变量
            Variable timeVar = openNC.findVariable("time");
            int[] time = (int[]) timeVar.read().copyTo1DJavaArray();

            //ssh变量
            Variable sshVar = openNC.findVariable("ssh");
            int[] ssh = (int[]) sshVar.read().copyTo1DJavaArray();

            List<StationPredictNcParseResp> list = new ArrayList<>(time.length * stationNumber.length);
            for (int i = 0; i < time.length; i++) {
                for (int j = 0; j < stationNumber.length; j++) {
                    StationPredictNcParseResp resp = new StationPredictNcParseResp();
                    resp.setLevel(Double.valueOf(ssh[stationNumber.length * i + j] / (float) 100000));
                    resp.setStationNumber(stationNumber[j]);
                    resp.setTime(new Date((long) time[i] * 1000));
                    list.add(resp);
                }
            }
            return list;

        } catch (Exception e) {
            log.error("解析数据失败", e);
            throw MyException.newException("解析数据失败");
        } finally {
            closeOpenNc(openNC);
        }
    }


    /**
     * 读取警戒预警信息
     * MAP<时间:<编号:数值>>
     * <p>
     * //读取time变量
     * //读取warning_level 变量 获取第二个行
     * //读取ssh那一行的值，
     * //遍历返回
     * <p>
     * * 读取堤坝预警信息
     * * MAP<时间:<编号:数值>>
     * * <p>
     * * //读取time变量
     * * //读取wol 变量 获取第二个行
     * * //读取ssh那一行的值，
     * * //遍历返回
     *
     * @param url  远程文件路径
     * @param time 时间
     * @return
     */
//    public static Map<Long, Map<Integer, List<Float>>> getStormWarnTide(String url, Long time) {
    public static Map<Integer, List<Float>> getStormWarnTide(String url, Long time) {
        //TODO 加入缓存，根据参数和方法
        NetcdfFile openNC = null;
        try {
            openNC = getNetcdfFile(url);

            //time变量
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();
            int timeIndex = getTimeIndex(time, timeValue);

            //ssh变量
            Variable sshVar = openNC.findVariable("ssh");
            int[] sshValue = new int[0];
            sshValue = (int[]) sshVar.read().copyTo1DJavaArray();
            Dimension sshDimension = sshVar.getDimension(1);

            //warn_level变量
            Variable warningLevelVar = openNC.findVariable("warning_level");
            int[] warningLevelValue = new int[0];
            warningLevelValue = (int[]) warningLevelVar.read().copyTo1DJavaArray();

            Dimension warnLevelDimension = warningLevelVar.getDimension(1);

            Map<Long, Map<Integer, List<Float>>> result = new HashMap<>(timeValue.length);

            //时间纬度
//            for (int i = 0; i < timeValue.length; i++) {

            Map<Integer, List<Float>> warnMap = new HashMap<>();

            //警戒纬度
            for (int j = 0; j < warningLevelValue.length; j += warnLevelDimension.getLength()) {

                List<Float> datas = new ArrayList<>(5);
                //警戒编号
                int warnPointNumber = warningLevelValue[j];

                //ssh目标行
                int sshLineNumber = warningLevelValue[j + 1];

                int index = timeIndex * sshDimension.getLength() + sshLineNumber;
                if (index <= sshValue.length) {

                    int ssh = sshValue[index];

                    datas.add((((float) (ssh) / 100000)));
                    datas.add((((float) (warningLevelValue[j + 2]) / 100)));
                    datas.add((((float) (warningLevelValue[j + 3]) / 100)));
                    datas.add((((float) (warningLevelValue[j + 4]) / 100)));
                    datas.add((((float) (warningLevelValue[j + 5]) / 100)));

                    //ssh纬度
                    warnMap.put(warnPointNumber, datas);
                }
            }
//            result.put(timeValue[timeIndex] * 1000L, warnMap);
//            }
//            return result;
            return warnMap;
        } catch (Exception e) {
            log.error("解析警戒预警数据失败", e);
            throw MyException.newException("解析警戒预警数据失败");
        } finally {
            closeOpenNc(openNC);
        }
    }

    /**
     * 获取时间变量数组对应时间戳的下标
     *
     * @param time
     * @param timeValue
     * @return
     */
    public static int getTimeIndex(Long time, int[] timeValue) {

        //判断时间戳是秒级还是毫秒级，毫秒级需要转成秒，因为timeValues是秒
        String timeStr = Convert.toStr(time);
        //毫秒
        if(timeStr.length() == 13){
            time = Convert.toLong(timeStr.substring(0,10));
        }

        //获取读取的时间变量下标
        int timeIndex = -1;
        for (int t = 0; t < timeValue.length; t++) {
            if (timeValue[t] == time) {
                timeIndex = t;
                break;
            }
        }
        if (timeIndex == -1) {
            throw MyException.newException("当前时间点不存在警戒预警信息");
        }
        return timeIndex;
    }


    public static Map<Integer, List<Float>> getStormDykeTide(String url, Long time) {
        //TODO 加入缓存，根据参数和方法
        NetcdfFile openNC = null;
        try {
            openNC = getNetcdfFile(url);

            //time变量
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();

            //获取读取的时间变量下标
            int timeIndex = getTimeIndex(time, timeValue);

            //ssh变量
            Variable sshVar = openNC.findVariable("ssh");
            int[] sshValue = new int[0];
            sshValue = (int[]) sshVar.read().copyTo1DJavaArray();
            Dimension sshDimension = sshVar.getDimension(1);

            //wol变量
            Variable wolVar = openNC.findVariable("wol");
            int[] wolValue = new int[0];
            wolValue = (int[]) wolVar.read().copyTo1DJavaArray();

            Dimension warnLevelDimension = wolVar.getDimension(1);

            Map<Long, Map<Integer, List<Float>>> result = new HashMap<>(timeValue.length);

            //时间纬度
//            for (int i = 0; i < timeValue.length; i++) {
            Map<Integer, List<Float>> warnMap = new HashMap<>();
            //漫堤预警
            for (int j = 0; j < wolValue.length; j += warnLevelDimension.getLength()) {
                List<Float> datas = new ArrayList<>(5);
                //堤坝编号
                int wolPointNumber = wolValue[j];
                //ssh目标行
                int sshLineNumber = wolValue[j + 1];

                int index = timeIndex * sshDimension.getLength() + sshLineNumber;
                if (index <= sshValue.length) {
                    int ssh = sshValue[index];
                    datas.add((((float) (ssh) / 100000)));
                    datas.add((((float) (wolValue[j + 2]) / 100)));
                    datas.add((((float) (wolValue[j + 3]) / 100)));
                    datas.add((((float) (wolValue[j + 4]) / 100)));
                    datas.add((((float) (wolValue[j + 5]) / 100)));
                    //ssh纬度
                    warnMap.put(wolPointNumber, datas);
                }
            }
//                result.put(timeValue[i] * 1000L, warnMap);

//            }
            return warnMap;
        } catch (Exception e) {
            log.error("解析数据失败", e);
            throw MyException.newException("解析数据失败");
        } finally {
            closeOpenNc(openNC);
        }
    }

    /**
     * 关闭openNc
     *
     * @param openNC
     */
    private static void closeOpenNc(NetcdfFile openNC) {
        if (openNC != null) {
            try {
                openNC.close();
            } catch (IOException e) {
                log.error("关闭opendap失败", e);
                throw MyException.newException("关闭opendap失败");
            }
        }
    }

    /**
     * 通过编号获取 警戒潮位折线图信息
     *
     * @param url
     * @param number
     * @return
     */
    public static StormBigScreenWarnTideChart getStormWarnTideChart(String url, Integer number) {

        StormBigScreenWarnTideChart stormBigScreenWarnTideChart = new StormBigScreenWarnTideChart();

        //TODO 加入缓存，根据参数和方法
        NetcdfFile openNC = null;
        try {
            openNC = getNetcdfFile(url);

            //warn_level变量
            Variable warningLevelVar = openNC.findVariable("warning_level");
            int[] warningLevelValue = new int[0];
            warningLevelValue = (int[]) warningLevelVar.read().copyTo1DJavaArray();

            Dimension warnLevelDimension = warningLevelVar.getDimension(1);


            int targetNumber = -1;

            //目标下标
            int targetNumberPoint = -1;

            //查找警戒编号是否存在
            for (int j = 0; j < warningLevelValue.length; j += warnLevelDimension.getLength()) {
                //警戒编号
                if (warningLevelValue[j] == number) {
                    targetNumber = number;
                    targetNumberPoint = j;
                    break;
                }
            }

            if (targetNumber == -1) {
                throw MyException.newException("警戒编号不存在");
            }


            //time变量
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();

            //ssh变量
            Variable sshVar = openNC.findVariable("ssh");
            int[] sshValue = new int[0];
            sshValue = (int[]) sshVar.read().copyTo1DJavaArray();
            Dimension sshDimension = sshVar.getDimension(1);

            List<StormWarnTideChartResp> predictLevels = new ArrayList<>();
            List<StormWarnTideChartResp> waterLevels = new ArrayList<>();
            List<StormWarnTideChartResp> tideLevels = new ArrayList<>();

            //时间纬度
            for (int i = 0; i < timeValue.length; i++) {

                //ssh目标行
                int sshLineNumber = warningLevelValue[targetNumber + 1];

                int index = i * sshDimension.getLength() + sshLineNumber;
                if (index <= sshValue.length) {

                    int ssh = sshValue[index];

                    StormWarnTideChartResp stormWarnTideChartResp = new StormWarnTideChartResp();
                    stormWarnTideChartResp.setLevel((((float) (ssh) / 100000)));
                    stormWarnTideChartResp.setTime(new Date(timeValue[i] * 1000L));
                    predictLevels.add(stormWarnTideChartResp);

                    //TODO 假数据填充
                    waterLevels.add(stormWarnTideChartResp);
                    tideLevels.add(stormWarnTideChartResp);

                }
            }

            stormBigScreenWarnTideChart.setBlue((float) (warningLevelValue[number + 2]) / 100);
            stormBigScreenWarnTideChart.setYellow((((float) (warningLevelValue[number + 3]) / 100)));
            stormBigScreenWarnTideChart.setOrange((((float) (warningLevelValue[number + 4]) / 100)));
            stormBigScreenWarnTideChart.setRed(((((float) (warningLevelValue[number + 5]) / 100))));

            stormBigScreenWarnTideChart.setPredictLevel(predictLevels);

            stormBigScreenWarnTideChart.setTideLevel(tideLevels);

            stormBigScreenWarnTideChart.setWaterLevel(waterLevels);

            return stormBigScreenWarnTideChart;
        } catch (
                Exception e) {
            log.error("解析警戒预警数据失败", e);
            throw MyException.newException("解析警戒预警数据失败");
        } finally {
            closeOpenNc(openNC);
        }
    }

    /**
     * 漫堤预报-折线图
     *
     * @param url
     * @param number
     * @return
     */
    public static StormBigScreenDykeTideChart getStormDykeTideChart(String url, Integer number) {
        //TODO 加入缓存，根据参数和方法
        NetcdfFile openNC = null;
        try {
            openNC = getNetcdfFile(url);

            //wol变量
            Variable wolVar = openNC.findVariable("wol");
            int[] wolValue = new int[0];
            wolValue = (int[]) wolVar.read().copyTo1DJavaArray();
            Dimension warnLevelDimension = wolVar.getDimension(1);

            //堤坝编号
            int targetNumber = -1;

            //目标下标
            int targetNumberPoint = -1;


            //查找堤坝编号是否存在
            for (int j = 0; j < wolValue.length; j += warnLevelDimension.getLength()) {
                //堤坝编号
                if (wolValue[j] == number) {
                    targetNumber = number;
                    targetNumberPoint = j;
                    break;
                }
            }

            if (targetNumber == -1) {
                throw MyException.newException("堤坝预警编号不存在");
            }

            StormBigScreenDykeTideChart stormBigScreenDykeTideChart = new StormBigScreenDykeTideChart();
            //预测水位
            List<StormDykeTideChartResp> predictLevels = new ArrayList<>();
            //增水
            List<StormDykeTideChartResp> waterLevels = new ArrayList<>();
            //潮位
            List<StormDykeTideChartResp> tideLevels = new ArrayList<>();

            //time变量
            Variable timeVar = openNC.findVariable("time");
            int[] timeValue = new int[0];
            timeValue = (int[]) timeVar.read().copyTo1DJavaArray();

            //ssh变量
            Variable sshVar = openNC.findVariable("ssh");
            int[] sshValue = new int[0];
            sshValue = (int[]) sshVar.read().copyTo1DJavaArray();
            Dimension sshDimension = sshVar.getDimension(1);

            //时间纬度
            for (int i = 0; i < timeValue.length; i++) {
                //漫堤预警

                //ssh目标行
                int sshLineNumber = wolValue[targetNumberPoint + 1];

                int index = i * sshDimension.getLength() + sshLineNumber;
                if (index <= sshValue.length) {
                    int ssh = sshValue[index];

                    StormDykeTideChartResp stormDykeTideChartResp = new StormDykeTideChartResp();
                    stormDykeTideChartResp.setTime(new Date(timeValue[i] * 1000L));
                    stormDykeTideChartResp.setLevel((((float) (ssh) / 100000)));

                    predictLevels.add(stormDykeTideChartResp);

                    //TODO 假数据填充
                    waterLevels.add(stormDykeTideChartResp);
                    tideLevels.add(stormDykeTideChartResp);

                }

                stormBigScreenDykeTideChart.setBlue(((float) (wolValue[targetNumber + 2]) / 100));
                stormBigScreenDykeTideChart.setYellow(((float) (wolValue[targetNumber + 3]) / 100));
                stormBigScreenDykeTideChart.setOrange(((float) (wolValue[targetNumber + 4]) / 100));
                stormBigScreenDykeTideChart.setRed(((float) (wolValue[targetNumber + 5]) / 100));

                stormBigScreenDykeTideChart.setPredictLevel(predictLevels);
                stormBigScreenDykeTideChart.setWaterLevel(waterLevels);
                stormBigScreenDykeTideChart.setTideLevel(tideLevels);
            }
            return stormBigScreenDykeTideChart;
        } catch (Exception e) {
            log.error("解析数据失败", e);
            throw MyException.newException("解析数据失败");
        } finally {
            closeOpenNc(openNC);
        }
    }
}
