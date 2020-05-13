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
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FilePipeline extends FilePersistentBase implements Pipeline, Closeable {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${Pipeline.fileName}")
    private String fileName;

    @Value("${resultItems.key}")
    private String key;

    @Value("${resultItems.modificationTime}")
    private String modificationTime;

    private ConcurrentHashMap<String, Object> fslosData;

    private AtomicReference<String> oldModificationTimeStr = new AtomicReference<>();

    private Path path;

    @PostConstruct
    private void init() throws IOException {
        path = Paths.get(System.getProperty("user.dir")).resolve(fileName);
        byte[] bytes = Files.readAllBytes(path);
        fslosData = new ConcurrentHashMap<>((Map<String, Object>) JSON.parse(new String(bytes, StandardCharsets.UTF_8)));
        String oldModificationTime = (String) fslosData.get(modificationTime);
        oldModificationTimeStr.set(oldModificationTime);
    }


    @Override
    public void process(ResultItems resultItems, Task task) {
        logger.info("start persistence ==============================");
        logger.info("Request.url = " + resultItems.getRequest().getUrl());
        String modificationTimeStr = resultItems.get(modificationTime);
        Pattern pattern = Pattern.compile("(.{4})-(.{1,2})-(.{1,2})");
        Matcher matcher = pattern.matcher(modificationTimeStr);
        if (matcher.find()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            Map<String, Object> fslosDataOfyear = (Map<String, Object>) fslosData.get(year);
            Integer addData = Integer.valueOf(resultItems.get(key));
            logger.info("modificationTimeStr = " + modificationTimeStr + ", addData = " + addData);
            if (fslosDataOfyear == null) {
                fslosDataOfyear = new ConcurrentHashMap<>();
                fslosDataOfyear.put(month, addData);
                fslosData.put(year, fslosDataOfyear);
            } else {
                Integer housingSalesForMonth = (Integer) fslosDataOfyear.get(month);
                logger.info("modificationTimeStr = " + modificationTimeStr + ", housingSalesForMonth = " + housingSalesForMonth);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (housingSalesForMonth == null) {
                    fslosDataOfyear.put(month, Integer.valueOf(resultItems.get(key)));
                } else {
                    fslosDataOfyear.put(month, (int) housingSalesForMonth + Integer.valueOf(resultItems.get(key)));
                }
            }
            oldModificationTimeStr.accumulateAndGet(modificationTimeStr, (preV, curV) -> {
                if (compareTime(curV, preV)) {
                    return curV;
                }
                return preV;
            });
        }
        logger.info("end persistence ==============================");
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

    /**
     * @description: 销毁时候，写入数据
     * 5/13/2020 modified by canno30
     */
    @Override
    public void close() throws IOException {
        try (PrintWriter printWriter = new PrintWriter(path.toFile())) {
            fslosData.put(modificationTime, oldModificationTimeStr.get());
            printWriter.write(JSON.toJSONString(fslosData));
        }
    }
}
