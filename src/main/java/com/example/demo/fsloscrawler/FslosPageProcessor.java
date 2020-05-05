package com.example.demo.fsloscrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

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

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent("Apache-HttpClient/4.5.12 (Java/1.8.0_191)");

    @Override
    public void process(Page page) {
        logger.debug("url = " + page.getUrl().toString());
        page.addTargetRequest(page.getHtml().xpath("//div[@class='npic']//a/@href").toString());
        page.putField(key, page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").match() ? page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").toString().trim() : null);
        // 如果没有totalNum字段就跳过这个页面，不要持久化 modified by Administrator on 2020/5/5Fi
        if (page.getResultItems().get(key) == null) {
            page.setSkip(true);
        }
        page.putField(modificationTime, page.getHtml().xpath("//head/title/text()").regex("(.{4})年(.{1,2})月(.{1,2})日", 0).toString());
    }

    @Override
    public Site getSite() {
        return site;
    }
}
