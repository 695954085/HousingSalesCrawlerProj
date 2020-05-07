package com.example.demo;

import com.example.demo.controller.HousingSalesDataAction;
import com.example.demo.fsloscrawler.FilePipeline;
import com.example.demo.fsloscrawler.FslosPageProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import us.codecraft.webmagic.Spider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class HousingSalesCrawlerProjApplicationTests {

    @Autowired
    private FslosPageProcessor fslosPageProcessor;

    @Autowired
    private FilePipeline filePipeline;

    @Value("${crawlerUrl}")
    private String crawlerUrl;

    @Autowired
    private MockMvc mvc;

    @Test
    void contextLoads() {
        Spider.create(fslosPageProcessor).addPipeline(filePipeline).addUrl(crawlerUrl).thread(5).run();
    }

    @Test
    void readFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("fslos.json");
        byte[] bytes = Files.readAllBytes(classPathResource.getFile().toPath());
        System.out.println(new String(bytes, "utf-8"));
    }

    @Test
    void readRegExp() {
        String title = "2020年5月4日佛山房产交易情况_佛山楼市网";
        String pattern = "(.{4})年(.{1,2})月(.{1,2})日";
        Pattern r = Pattern.compile(pattern);
        Matcher matcher = r.matcher(title);
        if (matcher.find()) {
            String year = matcher.group(1);
            String month = matcher.group(2);
            String day = matcher.group(3);
            System.out.println(year + " " + month + " " + day);
        }
    }

    @Test
    void housingSalesData() throws Exception {
//        mvc.perform(MockMvcRequestBuilders.get("/housingSalesData")).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(MockMvcResultMatchers.content().json());
    }

    @Test
    void whenUsingSystemProperties_thenReturnCurrentDirectory() {
        String userDirectory = System.getProperty("user.dir");
        System.out.println(userDirectory);
//        assertTrue(userDirectory.endsWith(CURRENT_DIR));
    }

    @Value("file:src/main/resources/${Pipeline.fileName}")
    private Resource resourceInSrc;

    @Test
    void testFileResource() throws IOException {
        File file = resourceInSrc.getFile();
        System.out.println(file.toPath().toString());
    }
}
