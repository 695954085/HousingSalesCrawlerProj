package com.example.demo;

import com.example.demo.controller.HousingSalesDataAction;
import com.example.demo.fsloscrawler.FilePipeline;
import com.example.demo.fsloscrawler.FslosPageProcessor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.SpiderListener;

import javax.swing.plaf.TableHeaderUI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

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

    private final Logger logger = LoggerFactory.getLogger(getClass());

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

    @Test
    void testDayDiffer() throws ParseException {
        String str = "2020年2月1日";
        String str1 = "2019年12月30日";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        Date parse = sdf.parse(str);
        Date parse1 = sdf.parse(str1);
        long diffInMillies = Math.abs(parse1.getTime() - parse.getTime());
        long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        System.out.println(diff);
    }

    @Test
    void getAvailableIDs() {
        String[] availableIDs = TimeZone.getAvailableIDs();
        System.out.println(availableIDs.length);
    }

    @Test
    void test() {
        String property = Paths.get(System.getProperty("user.dir")).resolve("fslos.json").toString();
        System.out.println(property);
    }

    private synchronized void test1() throws InterruptedException {
        System.out.println("test1 = " + new Date().getTime());
        Thread.sleep(6000);
    }

    private synchronized void test2() {
        System.out.println("test2 = " + new Date().getTime());
    }

    public static void main(String[] args) {
        HousingSalesCrawlerProjApplicationTests tests = new HousingSalesCrawlerProjApplicationTests();
        new Thread(() -> {
            try {
                tests.test1();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            tests.test2();
        }).start();
    }

    @Test
    void test3() {
        Map collect = Arrays.asList().stream().collect(Collectors.toMap(cst -> cst, cst -> new HashMap()));
        Assert.assertNotNull(collect);
    }

    @Test
    public void givenHashMap_whenSumParallel_thenError() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        List<Integer> sumList = parallelSum100(map, 100);

        assertNotEquals(1, sumList
                .stream()
                .distinct()
                .count());
        long wrongResultCount = sumList
                .stream()
                .filter(num -> num != 100)
                .count();

        assertTrue(wrongResultCount > 0);
    }

    private List<Integer> parallelSum100(Map<String, Integer> map,
                                         int executionTimes) throws InterruptedException {
        List<Integer> sumList = new ArrayList<>(1000);
        for (int i = 0; i < executionTimes; i++) {
            map.put("test", 0);
            ExecutorService executorService =
                    Executors.newFixedThreadPool(4);
            for (int j = 0; j < 10; j++) {
                executorService.execute(() -> {
                    for (int k = 0; k < 10; k++)
                        map.computeIfPresent(
                                "test",
                                (key, value) -> value + 1
                        );
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            sumList.add(map.get("test"));
        }
        return sumList;
    }

    @Test
    public void givenConcurrentMap_whenSumParallel_thenCorrect()
            throws Exception {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        List<Integer> sumList = parallelSum100(map, 1000);

        assertEquals(1, sumList
                .stream()
                .distinct()
                .count());
        long wrongResultCount = sumList
                .stream()
                .filter(num -> num != 100)
                .count();

        assertEquals(0, wrongResultCount);
    }

    @Test
    void test4() {
        Map<String, String> conMap = new ConcurrentHashMap<>();
        String key1 = conMap.computeIfPresent("key", (key, value) -> {
            return "xxx";
        });
        Assert.assertEquals(key1, "xxx");
    }

    @Test
    void test5() throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet("http://httpbin.org/");

            System.out.println("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            String responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("----------------------------------------");
            System.out.println(responseBody);
        } finally {
            httpclient.close();
        }
    }

}
