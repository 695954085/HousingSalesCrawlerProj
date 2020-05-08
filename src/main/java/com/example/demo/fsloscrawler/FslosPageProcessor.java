package com.example.demo.fsloscrawler;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

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
import java.util.List;
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
    private void init() throws IOException {
        // 1. 首先判断计算得到需要爬多少页面？
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.dir")).resolve(fileName));
            String str = new String(bytes, StandardCharsets.UTF_8);
            Map<String, Object> fslosData = (Map<String, Object>) JSON.parse(str);
            //  得到文件的记录数据日期，以此计算需要爬取的条目数量
            oldModificationTime = (String) fslosData.get(modificationTime);
        } catch (IOException e) {
            logger.error("json修改日期初始化异常，退出程序");
            throw e;
        }
    }

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent("Apache-HttpClient/4.5.12 (Java/1.8.0_191)").setCycleRetryTimes(3);

    @Override
    public void process(Page page) {
        logger.debug("url = " + page.getUrl().toString());
        // 证明是列表页面，开始计算需要下载详情页面数目 modified by Administrator on 2020/5/7
        // 列表页面的第一条数据
        String npictdate = page.getHtml().xpath("//div[@class='npic']//div[@class='npictxx']//text()").regex("(.{4})年(.{1,2})月(.{1,2})日", 0)
                .replace("年", "-").replace("月", "-").replace("日", "").toString();
        if (npictdate != null) {
            try {
                int fetchingNumber = calcDiffer(npictdate, oldModificationTime);
                Selectable xpath = page.getHtml().xpath("//div[@class='npic']//span[@id='tu']/a/@href");
                List<String> selectItems = xpath.all();
                if (fetchingNumber > selectItems.size()) {
                    // 需要翻页，获取下一页url modified by canno30 on 5/8/2020
                    page.addTargetRequest(page.getHtml().css("div#AspNetPager1 tr>td:nth-child(2)>a:nth-last-child(2)", "href").toString());
                }
                // 如果遇到fetchingNumber<selectItems时候需要注意 modified by canno30 on 5/8/2020
                for (int i = 0; i < fetchingNumber && i < selectItems.size(); i++) {
                    page.addTargetRequest(selectItems.get(i));
                }
                // 列表页面不需要数据持久化 modified by canno30 on 5/8/2020
                page.setSkip(true);
            } catch (ParseException e) {
                logger.error(e.getMessage(), e);
                return;
            }
        }
        // 获取详细页面的房屋销售数字 modified by canno30 on 5/8/2020
        page.putField(key, page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").match() ? page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").toString().trim() : null);
        page.putField(modificationTime, page.getHtml().xpath("//head/title/text()").regex("(.{4})年(.{1,2})月(.{1,2})日", 0)
                .replace("年", "-").replace("月", "-").replace("日", "").toString());
    }

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * 计算网站最新数据日期，以及当前记录日期的差值
     *
     * @return
     */
    private int calcDiffer(String theLatestDateStr, String theLastDateStr) throws ParseException {
        Date theLatestDate = simpleDateFormat.parse(theLatestDateStr);
        Date theLastDate = simpleDateFormat.parse(theLastDateStr);
        long diffInMillies = Math.abs(theLatestDate.getTime() - theLastDate.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        return (int) diff;
    }

    @Override
    public Site getSite() {
        return site;
    }
}
