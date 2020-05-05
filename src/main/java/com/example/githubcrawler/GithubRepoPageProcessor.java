package com.example.githubcrawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.JsonFilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;

public class GithubRepoPageProcessor implements PageProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Site site = Site.me().setRetryTimes(3).setSleepTime(1000).setUserAgent("Apache-HttpClient/4.5.12 (Java/1.8.0_191)");

    @Override
    public void process(Page page) {
//        page.putField("name", page.getHtml().xpath("//div[@class='npic']//a/@href").toString());
        page.addTargetRequest(page.getHtml().xpath("//div[@class='npic']//a/@href").toString());
        System.out.println(page.getUrl().toString());
        page.putField("totalNum", page.getHtml().xpath("//table//tr[3]/td[2]//span/text()").toString());
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        Spider.create(new GithubRepoPageProcessor()).addPipeline(new JsonFilePipeline("C:\\Users\\Administrator\\Desktop")).addUrl("http://www.fslos.com/news.aspx?id=102").thread(5).run();
    }
}
