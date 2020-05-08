package com.example.demo.fsloscrawler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FilePipeline extends FilePersistentBase implements Pipeline {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${Pipeline.fileName}")
    private String fileName;

    @Value("${resultItems.key}")
    private String key;

    @Value("${resultItems.modificationTime}")
    private String modificationTime;

    private Path path;

    @PostConstruct
    private void init() {
        path = Paths.get(System.getProperty("user.dir")).resolve(fileName);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        synchronized (this) {
            try {
                logger.info("start persistence ==============================");
                logger.info("Request.url = " + resultItems.getRequest().getUrl());
                byte[] bytes = Files.readAllBytes(path);
                Map<String, Object> fslosData = (Map<String, Object>) JSON.parse(new String(bytes, StandardCharsets.UTF_8));
                String modificationTimeStr = resultItems.get(modificationTime);
                String oldModificationTimeStr = (String) fslosData.get(modificationTime);
                if (oldModificationTimeStr.equals(modificationTimeStr)) {
                    logger.error("时间比对一致，无需更新, 时间为: " + modificationTimeStr + ", 请求路径为: " + resultItems.getRequest().getUrl());
                    return;
                }
                Pattern pattern = Pattern.compile("(.{4})-(.{1,2})-(.{1,2})");
                Matcher matcher = pattern.matcher(modificationTimeStr);
                if (matcher.find()) {
                    String year = matcher.group(1);
                    String month = matcher.group(2);
                    Map<String, Object> fslosDataOfyear = (Map<String, Object>) fslosData.get(year);
                    if (fslosDataOfyear == null) {
                        fslosDataOfyear = new HashMap<>();
                        fslosDataOfyear.put(month, Integer.valueOf(resultItems.get(key)));
                    } else {
                        Object housingSalesForMonth = fslosDataOfyear.get(month);
                        if (housingSalesForMonth == null) {
                            fslosDataOfyear.put(month, Integer.valueOf(resultItems.get(key)));
                        } else {
                            fslosDataOfyear.put(month, (int) housingSalesForMonth + Integer.valueOf(resultItems.get(key)));
                        }
                    }
                    if (compareTime(modificationTimeStr, oldModificationTimeStr)) {
                        fslosData.put(modificationTime, modificationTimeStr);
                    }
                    try (PrintWriter printWriter = new PrintWriter(path.toFile())) {
                        printWriter.write(JSON.toJSONString(fslosData));
                    }
                }
            } catch (IOException e) {
                logger.warn("write file error", e);
            } finally {
                logger.info("end persistence ==============================");
            }
        }
    }

    /**
     * @description: 如果大于0天，就修改json数据
     * 5/8/2020 modified by canno30
     */
    private boolean compareTime(String modificationTimeStr, String oldModificationTimeStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date parse = sdf.parse(modificationTimeStr);
            Date parse1 = sdf.parse(oldModificationTimeStr);
            long convert = TimeUnit.DAYS.convert(parse.getTime() - parse1.getTime(), TimeUnit.MILLISECONDS);
            return convert > 0;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
}
