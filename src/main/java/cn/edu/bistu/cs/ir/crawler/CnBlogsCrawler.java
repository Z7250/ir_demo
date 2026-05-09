package cn.edu.bistu.cs.ir.crawler;

import cn.edu.bistu.cs.ir.model.Blog;
import cn.edu.bistu.cs.ir.model.BlogStats;
import cn.edu.bistu.cs.ir.utils.HttpUtils;
import cn.edu.bistu.cs.ir.utils.StringUtil;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.processor.PageProcessor;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * 面向博客园（cnblogs.com）的爬虫
 * 例如：<a href="https://www.cnblogs.com/tencent-cloud-native/">腾讯云原生</a>
 * @author chenruoyu
 */
public class CnBlogsCrawler implements PageProcessor {


    private final Site site;

    /**
     * 博主的ID
     */
    private final String bloggerId;

    private static final Logger log = LoggerFactory.getLogger(CnBlogsCrawler.class);

    public static final String RESULT_ITEM_KEY = "BLOG_INFO";

    /**
     * 当前博主的博文目录页URL前缀
     */
    private final String list_prefix;

    /**
     * 当前博主的博文内容页URL前缀
     */
    private final String blog_prefix;

    //已经爬取的博文数量
    private int crawedCount = 0;

    //最大爬取博文数量；
    private int maxCount = 20;


    public CnBlogsCrawler(Site site, String bloggerId){
        this.site = site;
        this.bloggerId = bloggerId;
        //https://www.cnblogs.com/tencent-cloud-native?page=1
        //https://www.cnblogs.com/tencent-cloud-native/p/14913423.html
        this.list_prefix = String.format("https://www.cnblogs.com/%s?", bloggerId);
        this.blog_prefix = String.format("https://www.cnblogs.com/%s/p/", bloggerId);
    }

    private final SimpleDateFormat sdf = new SimpleDateFormat("uuuu-MM-dd HH:mm");

    @Override
    public void process(Page page) {
        //url里存储的是请求页面的URL地址
        String url = page.getRequest().getUrl();
        if(url.startsWith(blog_prefix)){

            if(crawedCount >= maxCount){
                log.info("已爬取[{}]，达到最大爬取数[{}]，跳过当前博文", crawedCount, maxCount);
                page.setSkip(true);
                return;
            }
            log.info("解析博客内容页[{}]", url);
            //博文的ID，设置为页面URL删去前缀和 .html后缀后的字符串
            String id = url.replace(blog_prefix, "").replace(".html", "");
            String title = page.getHtml().xpath("//div[@class='post']/h1[@class='postTitle']//span/text()").get();
            String time = page.getHtml().xpath("//div[@class='postDesc']/span[@id='post-date']/text()").get();
            String content = page.getHtml().xpath("//div[@class='post']//div[@id='cnblogs_post_body']/allText()").get();
            //TODO 请大家思考如何抓取页面中的标签、阅读数、评论数等数据?
            Blog blog = new Blog();
            blog.setId(id);
            blog.setTitle(title);
            blog.setContent(content);
            blog.setAuthor(bloggerId);
            HttpUtils httpUtils = new HttpUtils();
            String json = httpUtils.postJson(
                    String.format("https://www.cnblogs.com/%s/ajax/GetPostStat", bloggerId),
                    String.format("[%s]", id),
                    null);
            if(!StringUtil.isEmpty(json)){
                List<BlogStats> blogStats = JSONObject.parseArray(json, BlogStats.class);
                if(blogStats!=null&&blogStats.size()>0){
                    log.info("成功获取博文[{}]的阅读数等信息:[{}]", id, json);
                    blog.setBlogStats(blogStats.get(0));
                    crawedCount++;
                    log.info("已爬取[{}]", crawedCount);
                }
            }
            try {
                blog.setDate(sdf.parse(time).getTime());
            } catch (ParseException e) {
                log.error("无法识别的日期时间格式:[{}]", time);
                e.printStackTrace();
                blog.setDate(0);
            }
            page.putField(RESULT_ITEM_KEY, blog);
        }else if(url.startsWith(list_prefix)){
            if(crawedCount >= maxCount){
                log.info("已爬取[{}]，达到最大爬取数[{}]，停止爬取", crawedCount, maxCount);
                page.setSkip(true);
                return;
            }
            log.info("解析博客目录页[{}]", url);
            List<String> blogs = page.getHtml().xpath("//div[@class='forFlow']//div[@class='postTitle']/a/@href").all();
            page.addTargetRequests(blogs);
            int currentPage = currentPage(url);
            log.info("第[{}]页，内含博文[{}]条", currentPage, blogs.size());
            //仅在未达上限时添加下一页
            if(crawedCount + blogs.size() < maxCount){
                List<String> pages = page.getHtml().xpath("//div[@class='pager']/a/@href").all();
                pages.removeIf(p -> currentPage(p) != currentPage + 1);
                log.info("获取分页链接[{}]条", pages.size());
                page.addTargetRequests(pages);
            }
            page.setSkip(true);
        }else{
            log.warn("暂不支持的URL地址:[{}]", url);
            page.setSkip(true);
        }
    }

    /**
     * 从URL中提取页码，若无page参数则返回第1页
     */
    private static int currentPage(String url) {
        int idx = url.indexOf("page=");
        if (idx < 0) {
            return 1;
        }
        String page = url.substring(idx + 5);
        int amp = page.indexOf('&');
        if (amp > 0) {
            page = page.substring(0, amp);
        }
        try {
            return Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    public Site getSite() {
        return this.site;
    }
}
