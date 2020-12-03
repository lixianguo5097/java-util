package com.hqx.controller;

/**
 * @author LXG
 * @date 2020-3-3
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hqx.data.model.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author: brbai
 * @create: 2019-07-09 11:29:41
 * @description:
 */
@Component
@Slf4j
public class CSVUtils {
    /**
     * @Description: 把数据从csv文件中读取到list
     * @Param: [reader] 字符缓冲流
     * @return: java.util.List 读取到的数据（一次读取1000行）
     */
    public static List<String> readCSV(String fileName){
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName));){
            List<String> list = new ArrayList<>();
            String line = null;
            for(int i= 0; i < 10000; i++) {
                line = reader.readLine();
                if(line == null){
                    break;
                }
            }
            return list;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

