package com.example.demo.fsloscrawler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * description: 佛山楼市网的页面抓取类
 * 2020/5/5 modified by Administrator
 **/
@Component
public class FslosPageProcessor implements PageProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${resultItems.key}")
    private String key;

    @Value("${resultItems.modificationTime}")
    private String modificationTime;

    @Value("${Pipeline.fileName}")
    private String fileName;

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent("Apache-HttpClient/4.5.12 (Java/1.8.0_191)");

    @Override
    public void process(Page page) {
        logger.debug("url = " + page.getUrl().toString());
        // 1. 首先判断计算得到需要爬多少页面？
        String str;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("./" + fileName));
            str = new String(bytes, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return;
        }
        Map<String, Object> data = (Map<String, Object>) JSON.parse(str);
        //  得到文件的记录数据日期，以此计算需要爬取的条目数量
        String oldModificationTime = (String) data.get(modificationTime);

        page.addTargetRequest(page.getHtml().xpath("//div[@class='npic']//span[@id='tu']/a/@href").toString());
        page.putField(key, page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").match() ? page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").toString().trim() : null);
        // 如果没有totalNum字段就跳过这个页面，不要持久化 modified by Administrator on 2020/5/5
        if (page.getResultItems().get(key) == null) {
            page.setSkip(true);
        }
        page.putField(modificationTime, page.getHtml().xpath("//head/title/text()").regex("(.{4})年(.{1,2})月(.{1,2})日", 0).toString());
    }

    /**
     * 计算网站最新数据日期，以及当前记录日期的差值
     * @return
     */
    private int calcDiffer(String theLastestDay, String oldDay) {
        return 0;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
