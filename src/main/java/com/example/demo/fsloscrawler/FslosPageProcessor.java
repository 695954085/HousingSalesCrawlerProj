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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

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

    private String oldModificationTime;

    @PostConstruct
    private void init() {
        // 1. 首先判断计算得到需要爬多少页面？
        try {
            byte[] bytes = Files.readAllBytes(Paths.get("./" + fileName));
            String str = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Object> fslosData = (Map<String, Object>) JSON.parse(str);
            //  得到文件的记录数据日期，以此计算需要爬取的条目数量
            oldModificationTime = (String) fslosData.get(modificationTime);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent("Apache-HttpClient/4.5.12 (Java/1.8.0_191)");

    @Override
    public void process(Page page) {
        logger.debug("url = " + page.getUrl().toString());
        // 证明是列表页面，开始计算需要下载详情页面数目 modified by Administrator on 2020/5/7
        String npictdate = page.getHtml().xpath("//div[@class='npic']//div[@class='npictdate']//text()").toString();
        if (npictdate != null) {
            try {
                int fetchingNumber = calcDiffer(npictdate, oldModificationTime);
                
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        page.addTargetRequest(page.getHtml().xpath("//div[@class='npic']//span[@id='tu']/a/@href").toString());
        page.putField(key, page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").match() ? page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").toString().trim() : null);
        // 如果没有totalNum字段就跳过这个页面，不要持久化 modified by Administrator on 2020/5/5
        if (page.getResultItems().get(key) == null) {
            page.setSkip(true);
        }
        page.putField(modificationTime, page.getHtml().xpath("//head/title/text()").regex("(.{4})年(.{1,2})月(.{1,2})日", 0).toString());
    }

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    private SimpleDateFormat simpleDateFormatForChineseCharacter = new SimpleDateFormat("yyyy年MM月dd日");

    /**
     * 计算网站最新数据日期，以及当前记录日期的差值
     *
     * @return
     */
    private int calcDiffer(String theLatestDateStr, String theLastDateStr) throws ParseException {
        Date theLatestDate = simpleDateFormat.parse(theLatestDateStr);
        Date theLastDate = simpleDateFormatForChineseCharacter.parse(theLastDateStr);
        long diffInMillies = Math.abs(theLatestDate.getTime() - theLastDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        return (int) diff;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
