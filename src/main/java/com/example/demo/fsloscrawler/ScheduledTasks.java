package com.example.demo.fsloscrawler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Spider;

@Component
public class ScheduledTasks {

    @Autowired
    private FslosPageProcessor fslosPageProcessor;

    @Autowired
    private FilePipeline filePipeline;

    @Value("${crawlerUrl}")
    private String crawlerUrl;

    @Scheduled(cron = "0 0 9 * * *")
    public void crawler() {
        Spider.create(fslosPageProcessor).addPipeline(filePipeline).addUrl(crawlerUrl).thread(20).run();
    }
}
