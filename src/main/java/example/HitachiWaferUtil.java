/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package example;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.control.TextArea;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Component
public class HitachiWaferUtil {

    @Autowired
    HelloworldController helloworldController;

    //    static String waferMappingPath = "D:\\WAFERMAPPING";
//    static String waferSavePath = "D:\\WAFERMAPPING_RESULT";
    private static final Logger logger = Logger.getLogger(HitachiWaferUtil.class);
    private static Properties prop;

    public void waferParse(File in, String angle, String text, boolean isFirst) {
        loadBinConfig();
        if (in.isDirectory()) {
            File[] files = in.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    waferParse(file, angle, text, false);
                } else {
                    waferOneFile(file, angle, text, false);
                }
            }
        } else {
            waferOneFile(in, angle, text, isFirst);
        }


    }

    public void waferOneFile(File file, String angle, String text, boolean isFirst) {
        String fileName = file.getName();
        if (fileName.endsWith(".API") || fileName.endsWith(".api") || fileName.endsWith(".xls")) {
            return;
        }
        String path = file.getPath();
        logger.info(path);
        String replace = null;
        if (isFirst) {
            String temp = path.substring(path.lastIndexOf("."));
            replace = path.replace(temp, "_RESULT" + temp);
        } else {
            replace = path.replace(text, text + "_RESULT");
        }

        InputStream in = null;
        BufferedReader br = null;
        try {
            in = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String tmpString = "";
            List<String> list = new ArrayList<>();
            boolean multipleMap = false;  //一个文件是否包含多个Map
            List<List<String>> muliList = new ArrayList<>();
            List<String> outName = new ArrayList<>();

            if (fileName.toLowerCase().endsWith(".xml") || fileName.toLowerCase().endsWith(".txt")) {
                while ((tmpString = br.readLine()) != null) {
                    if (tmpString.contains("<Maps>") || tmpString.contains("<SubstrateMaps>")) {
                        multipleMap = true;
                        break;
                    }
                    list.add(tmpString);
                }
                if (multipleMap) {
                    boolean isWaferId = false;  //以WaferID 为文件命名优先
                    boolean flag = false;
                    boolean binCode = false;  //数据行的节点名称是否是<BinCode>
                    String firstName = null;
                    String firstNameTemp = null;

                    while ((tmpString = br.readLine()) != null) {
                        if (tmpString.contains("<Map")) {
                            flag = true;
                        } else if (tmpString.contains("<SubstrateMap")) {
                            flag = true;
                            binCode = true;
                        }
                        if (flag) {
                            if (tmpString.contains("WaferID")) {
                                String temp = tmpString.substring(tmpString.indexOf("WaferID") + 7);
                                String temp1 = temp.substring(temp.indexOf("\"") + 1);
                                firstName = temp1.substring(0, temp1.indexOf("\"")) + fileName.substring(fileName.lastIndexOf("."));
                                isWaferId = true;
                                break;

                            } else if (tmpString.contains("SubstrateId")) {
                                String temp = tmpString.substring(tmpString.indexOf("SubstrateId") + 11);
                                String temp1 = temp.substring(temp.indexOf("\"") + 1);
                                if (temp1.indexOf("\"") == 0) {
                                    temp1 = temp1.substring(1);
                                }
                                firstNameTemp = temp1.substring(0, temp1.indexOf("\"")) + fileName.substring(fileName.lastIndexOf("."));
                            }
                            if (tmpString.contains(">")) {
                                break;
                            }
                        }

                    }
                    if (isWaferId) {
                        outName.add(firstName);
                    } else {
                        outName.add(firstNameTemp);
                    }
                    muliList.add(new ArrayList<>());

                    while ((tmpString = br.readLine()) != null) {
                        if (isWaferId && tmpString.contains("WaferID")) {
                            String temp = tmpString.substring(tmpString.indexOf("WaferID") + 7);
                            String temp1 = temp.substring(temp.indexOf("\"") + 1);
                            firstName = temp1.substring(0, temp1.indexOf("\"")) + fileName.substring(fileName.lastIndexOf("."));
                            outName.add(firstName);
                            muliList.add(new ArrayList<>());
                        } else if ((!isWaferId) && tmpString.contains("SubstrateId")) {
                            String temp = tmpString.substring(tmpString.indexOf("SubstrateId") + 11);
                            String temp1 = temp.substring(temp.indexOf("\"") + 1);
                            if (temp1.indexOf("\"") == 0) {
                                temp1 = temp1.substring(1);
                            }
                            firstNameTemp = temp1.substring(0, temp1.indexOf("\"")) + fileName.substring(fileName.lastIndexOf("."));
                            outName.add(firstNameTemp);
                            muliList.add(new ArrayList<>());
                        }
                        muliList.get(muliList.size() - 1).add(tmpString);
//                        if ((!binCode) && tmpString.contains("<Row><![CDATA[")) {
//                            muliList.get(muliList.size() - 1).add(tmpString);
//                        } else if (binCode && tmpString.contains("<BinCode>")) {
//                            muliList.get(muliList.size() - 1).add(tmpString);
//                        } else {
//                            logger.error("解析错误！！！");
//                        }
                    }
                }
            } else {
                boolean flag = false;
                while ((tmpString = br.readLine()) != null) {
                    if (tmpString.contains("<WAFER_INFO>")) {
                        flag = true;
                    }
                    list.add(tmpString);
                    if (flag && tmpString.contains("</WAFER_INFO>")) {
                        break;
                    }
                }

            }

            if (multipleMap) {
                for (int i = 0; i < muliList.size(); i++) {
                    String outPath = replace.substring(0, replace.lastIndexOf(File.separator) + 1) + outName.get(i);
                    outOneFile(muliList.get(i), angle, outPath, outName.get(i));
                }
            } else {
                outOneFile(list, angle, replace, fileName);
            }

        } catch (Exception e) {
            logger.error("Exception", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void outOneFile(List<String> list, String angle, String outPath, String fileName) {

        FileOutputStream fos = null;
        BufferedWriter bw = null;
        try {
            if (list.size() == 0) {
                return;
            }
            try {
                list = parseList(list);
            } catch (Exception e) {
                logger.error("解析文件出错：", e);
                return;
            }

            int length = list.size();
            String[] bins = new String[length];

            for (int i = 0; i < length; i++) {
                bins[i] = list.get(i);
            }
            int rowCount = list.size();
            int col = list.get(0).length();
            String[] results = transferAngle(bins, angle, rowCount, col);
            File out = new File(outPath);
            fos = FileUtils.openOutputStream(out);
            bw = new BufferedWriter(new OutputStreamWriter(fos));
            bw.write("Wafer_ID : " + fileName + "\n");
            bw.write("Flat_Notch : Bottom" + "\n");
            bw.write("" + "\n");
            for (int i = 0; i < results.length; i++) {
                bw.write(results[i] + "\n");
            }
            bw.flush();
        } catch (Exception e) {
            logger.error("文件输出出错：", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<String> parseList(List<String> list) {
        List<Integer> numList = new ArrayList<>(); //记录所需行数
        List<Integer> indexList = new ArrayList<>(); //记录下标
        int temp = list.get(0).length();
        int count = 0;  //所需的行数
        //记录每行个数相等最多且连续的行
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (temp == s.length()) {
                count++;
                if (i == list.size() - 1) {
                    numList.add(count);
                    indexList.add(i);
                    count = 0;
                }
            } else if (count > 5) {
                numList.add(count);
                indexList.add(i - 1);
                count = 1;
            } else {
                count = 1;
            }
            temp = s.length();
        }
        int index = 0;   //所需行的最后一个下标
        if (numList.size() == 1) {
            count = numList.get(0);
            index = indexList.get(0);
        } else if (numList.size() > 1) {
            List<Integer> tempList = new ArrayList<>(numList);
            Collections.sort(tempList);
            count = tempList.get(tempList.size() - 1);
            index = numList.indexOf(count);  //如果所需行数 有两个且相等，则解析有问题，正常取行数多的
            index = indexList.get(index);
        } else {
            logger.error("出错了"); //没有连续两行个数相等的
        }
        for (int i = (index - count + 1); i <= (index - count + 6); i++) {
            String s = list.get(i);
            if (s.contains("+") && (s.indexOf("+") != s.lastIndexOf("+"))) {
                count = index - i;
                break;
            }
        }
        boolean flag = true;
        int start = (index - count + 1 + index) / 2;
        /**
         *    10|                 1  8  1  1  1  9  1  1  1  1  1  1  1  1  1  1  1  4  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1
         *    11|              1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1 99  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  3  1  1  8  1  1  1  1  1  1  1
         *    12|           1  9  1  1  1  9  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  8  1  4  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1  1  1  1  1  1  1
         *    13|           4  1  1  1  1  1  1  1  8  1  1  1  8  1  4  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  4  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1
         *    14|        1  1  1  1  1  1  9  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1  1  1  1  4  1  1  1
         *    15|        4  1  8  9  1  9  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  4  1  1  1  1  1
         *    16|        9  1  1  1  9  9  1  1  1  8  1  1  1  4  1  1  8  1  1  1  1  1  1  4  1  1  4  1  1  1  1  1  1  1  1  1  1  1  8  1  1  1  1  1  1  1  1  1  1  3  1  1  1  1  1  1  1  1  1  1  1  1
         *    17|        1  1
         *    剔除序号
         */
        temp = list.get(start).indexOf("|");
        int temp1 = list.get(start).indexOf("+");//取所需中间行的"-"和"+"位置
        if (temp <= 0 && temp1 < 0) {
            flag = false;
        } else {  //包含的话，说明格式如：   16|     11  ，剔除序号
            if (temp < 0) {
                temp = temp1;
            }
            for (int i = start; i <= (start + 5); i++) {
                String s = list.get(i);
                if (!(s.contains("|") || s.contains("+"))) {
                    flag = false;
                } else {
                    if (!(temp == s.indexOf("|") || temp == s.indexOf("+"))) {
                        flag = false;
                        logger.error("数据中有问题项");
                    }
                }
            }
        }
        if (flag) {
            for (int i = (index - count + 1); i <= index; i++) {
                list.set(i, list.get(i).substring(temp + 1));
            }
        }
        flag = true;
        int num = 0; //用以记录验证到了几个字符
        long l = Instant.now().toEpochMilli();
        indexList = new ArrayList<>();
        while (flag) {   //处理是否包含的序号，  有序号，则剔除
            num++;
            int[] ints = numHandle(list, index, count, num, indexList);
            if (ints[0] < (count / 2)) {
                flag = false;
                if (ints[1] != ints[2] || ints[0] != ints[3]) {
                    num--;  //直到截止列，如果截止列全是空格，则也移除
                }
            }
            if (num > 10) {  //有可能全是数字，没有序号
                num = 0;
                flag = false;
            }
        }
        /**
         *     000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000111111111111111111111111111111111111111111111111111111111111111111111111111111111
         *     000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999000000000011111111112222222222333333333344444444445555555555666666666677777777778
         *     123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890
         * 001                                                                                      1111141111
         * 002                                                                               111111111111111111111111
         * 003                                                                           11111111111111111111111111111111
         * 004
         * 移除上面三行
         */
        if (num > 1) {
            if (indexList.size() > 0) {
                Collections.sort(indexList);
                temp = indexList.get(0);
                temp1 = 1;
                List<Integer> integers = new ArrayList<>();
                for (int i = 1; i < indexList.size(); i++) {
                    Integer integer = indexList.get(i);  //4
                    if (integer == temp) {
                        temp1++;
                        if (i == indexList.size() - 1) {
                            integers.add(temp);
                        }
                    } else {
                        if (temp1 > 1) {
                            integers.add(temp);
                        }
                        temp1 = 1;
                    }
                    temp = integer;
                }
                if (integers.size() > 0) {
                    count = index - integers.get(integers.size() - 1);   //移除上面不需要的几行
                }
            }

            for (int i = (index - count + 1); i <= index; i++) {
                String s = list.get(i);
                list.set(i, s.substring(num));
            }
        }
        List<String> resultList = new ArrayList<>();
        indexList = new ArrayList<>();
        temp = 0;
        flag = false;

        int charNum = 1;
        int startIndex = 0;
        boolean noSpace = false;  //xml 是否需要移除空格
        String firstStr = list.get(index - count + 1);
        if (firstStr.contains("<Row><![CDATA[")) {
            startIndex = list.get(index - count + 1).indexOf("<Row><![CDATA[");
            for (int i = 0; i <= (index - count + 1); i++) {
                String s = list.get(i);
                if (s.contains("<Bin ")) {
                    int index1 = s.indexOf("BinCode=\"");
                    String substring = s.substring(index1 + 9);
                    if (substring.indexOf("\"") == 0) {//  为了处理第一个还是"的   例："1"" BinQuality=""Pass"" BinDescription=""Normal Pass"" BinCount=""1316""/>"
                        substring = substring.substring(1);
                    }
                    String substring1 = substring.substring(0, substring.indexOf("\""));
                    if (firstStr.substring(startIndex + 14, firstStr.length() - 9).contains(" ") && (!substring1.contains(" "))) {
                        noSpace = true;
                    }
                    charNum = substring1.length();
                    if (charNum == 0) {
                        charNum = 1;
                        logger.error("xml 多字符解析错误");
                    }
                    break;
                }
            }
        } else if (firstStr.contains("<BinCode>")) {
            startIndex = list.get(index - count + 1).indexOf("<BinCode>");
            for (int i = 0; i <= (index - count + 1); i++) {
                String s = list.get(i);
                if (s.contains("<BinDefinition ")) {
                    int index1 = s.indexOf("BinCode=\"");
                    String substring = s.substring(index1 + 9);
                    String substring1 = substring.substring(0, substring.indexOf("\""));
                    if (firstStr.substring(startIndex + 9, firstStr.length() - 10).contains(" ") && (!substring1.contains(" "))) {
                        noSpace = true;
                    }
                    charNum = substring1.length();
                    if (charNum == 0) {
                        charNum = 1;
                        logger.error("xml 多字符解析错误");
                    }
                    break;
                }
            }
        } else {
            //一些其他处理

        }
        //bincode解析

        /**
         * Y↓X→-1  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 64 65 66 67 68 69 70 71 72 73 74 75 76
         *    -1|
         *     0|
         *     1|
         *     2|                                                                                                                                                                                                                       A  A  A  C  C  C
         *     3|                                                                                                                                                                                                     A  A  B  C  P  C  C  C  C  C  C  P
         *     4|                                                                                                                                                                                         A  B  C  C  C  C  C  C  C  C  C  C  C  C  C  C
         *     5|                                                                                                                                                                             A  A  C  C  C  C  C  E  C  T  C  1  1  1  C  1  C  1  C  1
         *     6|                                                                                                                                                                    A  B  B  C  C  C  C  C  C  C  1  1  C  T  1  T  1  1  1  1  1  C  K
         *     7|                                                                                                                                                              B  B  C  C  C  C  C  K  E  C  C  1  1  C  C  C  1  1  1  1  T  K  1  1  C
         *     8|                                                                                                                                                     A  B  B  C  C  C  C  C  1  T  1  C  1  1  T  1  T  1  1  C  B  1  1  1  1  1  C  T
         *     9|                                                                                                                                               A  B  C  C  C  C  1  C  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  1  C  1  1  C
         *    10|                                                                                                                                         A  B  C  C  C  C
         *    移除上面的序号
         */
        int k = index - count + 1;
        for (int i = k; i <= (k + 2); i++) {
            String s = list.get(i);
            String[] s1 = s.split(" ");
            List<String> tempList = new ArrayList<>();
            for (String s2 : s1) {
                String trim = s2.trim();
                if (!StringUtils.isEmpty(trim)) {
                    tempList.add(trim);
                }
            }
            int size = 10;
            if (tempList.size() < size) {
                continue;
            }
            boolean delete = true;

            for (int i1 = 1; i1 < size; i1++) {
                String trim = tempList.get(i1);
                try {
                    int i2 = Integer.parseInt(trim);
                    if (i2 != (Integer.parseInt(tempList.get(i1 - 1)) + 1)) {
                        delete = false;
                        break;
                    }
                } catch (NumberFormatException e) {
                    delete = false;
                    break;
                }


            }
            if (delete) {
                count = count - (i - k + 1);
                break;
            }

        }

        /**
         *       111111111111111111111111111111111111111111111111111111111100
         *       555555554444444444333333333322222222221111111111000000000099
         *       765432109876543210987654321098765432109876543210987654321098
         *       ------------------------------------------------------------
         *                       MMMMMMMMMMMMMMMMMM
         *                       移除 -------------------及以上部分
         */

        for (int i = (index - count + 1); i <= index; i++) {
            String s = list.get(i);
            if (s.contains("-") || s.contains("+")) {
                flag = true;
                temp++;
                indexList.add(i);
            }
            if (s.contains("<Row><![CDATA[")) {
                String substring = s.substring(14 + startIndex, s.length() - 9);
                if (noSpace) {
                    substring = substring.replaceAll(" ", "");
                }
                list.set(i, substring);
            } else if (s.contains("<BinCode>")) {
                String substring = s.substring(9 + startIndex, s.length() - 10);
                if (noSpace) {
                    substring = substring.replaceAll(" ", "");
                }
                list.set(i, substring);
            }

            if (s.startsWith("RowData:")) {
                list.set(i, s.substring(8));
            }
        }
        if (flag && temp < (count / 4)) {
            if (indexList.size() == 1 && indexList.get(0) < (index - count + 8)) {
                count = index - indexList.get(0);
            } else if (indexList.size() == 1 && indexList.get(0) == index) { //最后一行为   ++-++-++-++-++  结束标志
                count--;
                index = index - temp;
            } else if (temp > 4) {
                logger.error("数据有问题，请核实");
            } else {
                temp1 = 0;
                temp = 0;
                for (int i = 0; i < indexList.size(); i++) {
                    Integer integer = indexList.get(i);
                    if (integer < (index - count + 8)) {
                        temp1++;
                    } else if (integer > (index - 4)) {
                        temp++;
                    } else {
                        logger.error("数据有问题，请核实");
                    }
                }
                if ((temp + temp1) == indexList.size()) {
                    if (temp > 0) {
                        index = index - temp;
                    }
                    if (temp1 > 0) {
                        count = index - indexList.get(temp1 - 1);
                    }
                } else {
                    logger.error("数据有问题，请核实");
                }
            }
        }
        Set<Integer> set = new HashSet<>();
        Set<Integer> set2 = new HashSet<>();
        Set<Integer> set3 = new HashSet<>();
        for (int i = (index - count + 1); i <= index; i++) {
            String s = list.get(i);
            if (!s.startsWith(" ")) {
                int index1 = s.indexOf(" ");
                if (index1 > 0) {
                    set.add(index1);
                }
                String substring = s.substring(index1 + 1);
                int index2 = substring.indexOf(" ");
                if (index2 > 0) {
                    set2.add(index2);
                    substring = substring.substring(index2 + 1);
                    int index3 = substring.indexOf(" ");
                    if (index3 > 0) {
                        set3.add(index3);
                    }
                }

            }
            resultList.add(list.get(i));
        }
        if (set.size() == 1 && set2.size() == 1 && set3.size() == 1) {//__ __ 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 __
            set = new HashSet<>();                                    //** FF11100FF11   除去空格前无效部分
            set.add(-1);
        }

        if (set.size() == 1) {
            for (Integer integer : set) {
                set = new HashSet<>();
                for (int i = 0; i < resultList.size(); i++) {
                    String s = resultList.get(i);
                    set.add(s.split(" ").length);
                    String substring = s.substring(integer + 1);
                    resultList.set(i, substring);
                }
            }
            if (set.size() == 1) {
                for (int i = 0; i < (index - count + 1); i++) {
                    String s = list.get(i);
                    if (s.contains("PASBIN")) {
                        int index1 = s.indexOf("PASBIN");
                        String substring = s.substring(index1 + 6);
                        substring = substring.trim();
                        String[] split = substring.split(",");
                        if (substring.contains(" ")) {
                            //如果包含空格另行处理
                            logger.info("没有添加的处理方式");
                        } else {
                            set = new HashSet<>();
                            for (int j = 0; j < split.length; j++) {
                                set.add(split[j].length());
                            }
                            if (set.size() == 1) {
                                for (Integer integer : set) {
                                    charNum = integer;  //多字符匹配
                                }
                            }
                        }
                    }
                }
            }

        }
        handleMultiCharacter(resultList, charNum);
        handleSpaceToSpot(resultList);
        return resultList;
    }

    private void handleSpaceToSpot(List<String> resultList) {
        String s1 = resultList.get(0);
        String substring1 = s1.substring(0, 1);
        String substring2 = s1.substring(s1.length() - 1);

        String s2 = resultList.get(resultList.size() - 1);
        String substring3 = s2.substring(0, 1);
        String substring4 = s2.substring(s2.length() - 1);


        if (substring1.equals(substring2) && substring2.equals(substring3) && substring3.equals(substring4)) {
            if (substring1.equals(".")) {
                return;
            }
            for (int i = 0; i < resultList.size(); i++) {
                String s = resultList.get(i);
                String temp = s.replaceAll(substring1, ".");
                resultList.set(i, temp);
            }
        } else if (substring1.equals(substring2)) {
            List<Integer> indexList = new ArrayList<>();
            int count = 1;
            for (int i = 1; i < resultList.size(); i++) {
                String s = resultList.get(i);
                String temp = s.substring(0, 1);
                if (temp.equals(substring1)) {
                    count++;
                } else {
                    break;
                }
            }
            if (count > (resultList.size() / 5)) {
                for (int i = resultList.size() - 1; i >= 0; i--) {
                    String s = resultList.get(i);
                    if (!s.substring(0, 1).equals(substring1)) {
                        indexList.add(i);
                    } else {
                        break;
                    }
                }
            } else {
                logger.error("出现特殊情况,与预期不一致，请联系开发人员处理");
            }
            for (int i = 0; i < indexList.size(); i++) {
                Integer integer = indexList.get(i);
                resultList.remove(integer.intValue());
            }
            if (substring1.equals(".")) {
                return;
            }
            for (int i = 0; i < resultList.size(); i++) {
                String s = resultList.get(i);
                String temp = s.replaceAll(substring1, ".");
                resultList.set(i, temp);
            }

        } else {
            logger.error("出现特殊情况，请联系开发人员处理");
        }

    }

    private void handleMultiCharacter(List<String> list, int charNum) {
        Map<String, String> binMap;
        binMap = transferKey2Map(String.valueOf(prop.get("ANY")));
        int index = list.size() / 2 - 2;
        int start = list.get(0).length() / 2 - 10;
        int num = 1; //默认为单字符的
        if (start >= 20) {
            List<Integer> indexList = new ArrayList<>(); //记录空串的坐标
            Set<Integer> set = new HashSet<>();
            for (int i = index; i < index + 4; i++) {
                String[] arr = list.get(i).substring(start, start + 20).split("");
                for (int j = 0; j < arr.length; j++) {
                    if (arr[j].equals(" ")) {
                        indexList.add(j);
                    }
                }
                if (indexList.size() == 0) {
                    continue;
                }
                int temp = indexList.get(0);
                boolean first = true;
                int firstIndex = 0;
                for (int j = 1; j < indexList.size(); j++) {
                    Integer integer = indexList.get(j);
                    if (first) {
                        if (integer - 1 > temp) {
                            first = false;
                            firstIndex = integer;
                        }
                    } else {
                        if (integer - 1 > temp) {
                            set.add(integer - firstIndex);
                            firstIndex = integer;
                        }
                    }
                    temp = integer;
                }
                indexList = new ArrayList<>();
            }
            if (set.size() == 1) {
                for (Integer integer : set) {
                    num = integer;
                }
                int temp = list.get(0).length() % num;
                if (temp != 0) {
//                int delNum = num - temp; //暂时判断删除一个空格
                    // 处理 类似于这种的多字符     "aaa aaa aaa"  ,无法按照标准切割
                    num = temp;
                    for (int i = 0; i < list.size(); i++) {
                        String s = list.get(i);
                        String str = s.substring(0, s.length() - num);
                        String end = s.substring(s.length() - num);
                        StringBuffer stringBuffer = new StringBuffer(str);
                        for (int j = num; j < stringBuffer.length(); j = j + num) {
                            stringBuffer.deleteCharAt(j);
                        }
                        stringBuffer.append(end);
                        list.set(i, stringBuffer.toString());
                    }
                }
                logger.info("处理多字符");
            } else if (set.size() > 1) {
                logger.error("处理多字符出错" + set);
            } else {
                num = charNum;//xml 处理多字符
//                logger.info("单字符");  //不一定是单字符
            }
        } else {
            num = charNum;//xml 处理多字符
            logger.info("单字符");
        }
        logger.info("用几个字符进行匹配：" + num);
        Set<String> set = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            int length = s.length();
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < length; j = j + num) {
                String sub = s.substring(j, j + num);
                String bin = binMap.get(sub);
                if (bin == null || "".equals(bin)) {
                    if (!set.contains(sub)) {
                        set.add(sub);
                        appendText(helloworldController.getText(), "没有匹配上的BinCode:<" + sub + ">");
                        logger.error("没有匹配上的BinCode:<" + sub + ">");
                    }
                    bin = " ";
                }
                sb.append(bin);
            }
            list.set(i, sb.toString());
        }
    }

    /**
     * 验证序号验证前几行
     *
     * @param list
     * @param index
     * @param count
     * @param num
     * @param indexList
     * @return
     */
    private int[] numHandle(List<String> list, int index, int count, int num, List<Integer> indexList) {
        int sucess = 0;
        int fail = 0;
        int spaceNum = 0;//表示空格数量
        int temp = index - count + 1;

        List<Integer> tempList = new ArrayList<>();
        for (int i = (index - count + 1); i <= index; i++) {
            String s = list.get(i);
            String substring = s.substring(num - 1, num);
            try {
                Integer.parseInt(substring);
                sucess++;
                if (temp == i) {
                    temp++;
                }
            } catch (NumberFormatException e) {
                fail++;
                tempList.add(i);
                if (substring.equals(" ")) {
                    spaceNum++;
                }
            }
        }
        if (sucess > (count / 2)) {
            indexList.addAll(tempList);  //成功的过多，怎加入indezList中，用以记录处理上面几行不需要的行
        }
        return new int[]{sucess, fail, spaceNum, temp - (index - count + 1)};

    }

    private String[] transferAngle(String[] src, String angle, int row, int col) {
        if ("0".equals(angle)) {
            return src;
        }
        String[] tmp = transferArgs1(src, row, col);
        if ("90".equals(angle)) {
            return tmp;
        }
        tmp = transferArgs1(tmp, col, row);
        if ("180".equals(angle)) {
            return tmp;
        }
        tmp = transferArgs1(tmp, row, col);
        if ("270".equals(angle)) {
            return tmp;
        }
        logger.error("旋转角度数值有误：" + angle);
        return null;
    }

    private static String[] transferArgs1(String[] src, int row, int col) {
        String[][] num = new String[row][col];
        for (int i = 0; i < row; i++) {
            String[] split = src[i].split("");
            for (int j = 0; j < col; j++) {
                num[i][j] = split[j];
            }
        }
        String[][] arr = new String[col][row];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                arr[j][i] = num[i][j];
            }
        }
        String[] result = new String[col];
        for (int i = 0; i < col; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = arr[i].length - 1; j >= 0; j--) {
                sb.append(arr[i][j]);
            }
            result[i] = sb.toString();
        }
        return result;
    }

//    private String[] getPath(String waferId, String deviceCode) {
//        String lot = waferId.split("-")[0];
//        String path = waferMappingPath + "\\" + deviceCode + "\\" + lot + "\\" + waferId;
//        String savePath = waferSavePath + "\\" + deviceCode + "\\" + lot + "\\" + waferId;
//        String[] arr = new String[]{path, savePath};
//
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tmb\\NF10A-08.tmb";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\out\\H00H37-08.out";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\asc\\HYMJC-01.asc";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\cp1\\RCSYN-18.CP1";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\dat\\SI11494-03.dat";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\EMT\\P1B122-01.EMT";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\ETC\\BPR296-18.ETC";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\smic\\SL1460-08.smic";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tma\\28-20111-0-01.tma";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\tmc\\862368-18.tmc";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\wfp\\S13195-18.wfp";
////         path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\WIN\\A738190-18.WIN";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\XML\\CP1042857-18.XML";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\UMC\\S1LP6-08.UMC";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\UTR\\P1M742-08.UTR";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\1.txt";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\2.txt";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\3.txt";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\4.txt";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\5.txt";
////         path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\6.txt";
////        path = "C:\\Users\\86180\\Desktop\\新建文件夹 (2)\\不同格式的MAP\\txt\\7.txt";
//        logger.info("waferpath：" + path);
//        return arr;
//    }

    private static void loadBinConfig() {
        FileInputStream fileInputStream = null;
        prop = new Properties();
        try {
            File file = new File("D://BinCode.properties");
            fileInputStream = new FileInputStream(file);
            prop.load(fileInputStream);
        } catch (Exception e) {
            logger.error("e", e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map transferKey2Map(String key) {
        Map map = new HashMap();
        if (StringUtils.isEmpty(key)) {
            logger.error("没有BinCode");
            return map;
        }
        String[] keys = key.split(",");
        for (String keyTmp : keys) {
            String[] values = keyTmp.split("=");
            map.put(values[0], values[1]);
        }

        return map;
    }

    public void appendText(TextArea logArea, String msg) {
        ObservableList<CharSequence> logs = logArea.getParagraphs();
        StringBuilder builder = new StringBuilder();
        if (logs.size() > 100) {
            ArrayList<CharSequence> tmpLogs = new ArrayList<>();
            tmpLogs.addAll(logs);
            tmpLogs.remove(logs.size() - 1);
            //超过指定数量删除
            ArrayList<CharSequence> tmpList = new ArrayList<>();
            for (int i = 0; i < 90; i++) {
                tmpList.add(logs.get(i));
            }
            tmpLogs.removeAll(tmpList);
            for (CharSequence str : tmpLogs) {
                builder.append(str.toString() + "\n");
            }
            logArea.clear();
        }
        builder.append(msg);
        Platform.runLater(() -> {
            logArea.appendText(builder.toString() + "\n");
        });
    }


}
