package com.example.demo.fsloscrawler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FilePipeline extends FilePersistentBase implements Pipeline {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${Pipeline.path}")
    private String path;

    @Value("${Pipeline.fileName}")
    private String fileName;

    @Value("${resultItems.key}")
    private String key;

    @Value("${resultItems.modificationTime}")
    private String modificationTime;

    @Value("classpath:${Pipeline.fileName}")
    private Resource resource;

    @PostConstruct
    private void init() {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        try {
            File file = resource.getFile();
            byte[] bytes = Files.readAllBytes(file.toPath());
            Map<String, Object> fslosData = (Map<String, Object>) JSON.parse(new String(bytes, StandardCharsets.UTF_8));
            String modificationTimeStr = resultItems.get(modificationTime);
            String oldModificationTimeStr = (String) fslosData.get(modificationTime);
            if (oldModificationTimeStr.equals(modificationTimeStr)) {
                logger.error("时间比对一致，无需更新, 时间为: " + modificationTimeStr + ", 请求路径为: " + resultItems.getRequest().getUrl());
                return;
            }
            Pattern pattern = Pattern.compile("(.{4})年(.{1,2})月(.{1,2})日");
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
                fslosData.put(modificationTime, modificationTimeStr);
                PrintWriter printWriter = new PrintWriter(file);
                printWriter.write(JSON.toJSONString(fslosData));
                printWriter.close();
            }
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
    }
}
