package cn.edu.bistu.cs.ir.index;

import cn.edu.bistu.cs.ir.crawler.CnBlogsCrawler;
//import cn.edu.bistu.cs.ir.crawler.SinaBlogCrawler;
import cn.edu.bistu.cs.ir.model.Blog;
import cn.edu.bistu.cs.ir.model.BlogStats;
import org.apache.lucene.document.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

/**
 * 基于Lucene的WebMagic Pipeline,
 * 用于将抓取的数据写入本地的Lucene索引
 * @author ruoyuchen
 */
public class LucenePipeline implements Pipeline {

    private static final Logger log = LoggerFactory.getLogger(LucenePipeline.class);

    private final IdxService idxService;
    public LucenePipeline(IdxService idxService){
        log.info("初始化LucenePipeline模块");
        this.idxService = idxService;
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        Blog blog = resultItems.get(CnBlogsCrawler.RESULT_ITEM_KEY);
        if(blog==null){
            log.error("无法从爬取的结果中提取到Blog对象");
            return;
        }
        String id = blog.getId();
        Document doc = toDoc(blog);
        boolean result = idxService.addDocument("ID", id, doc);
        if(!result){
            log.error("无法将ID为[{}]的博客内容写入索引", id);
        }
    }

    private Document toDoc(Blog blog){
        Document document = new Document();
        //页面ID
        document.add(new StringField("ID", blog.getId(), Field.Store.YES));
        //页面标题
        document.add(new TextField("TITLE", blog.getTitle(), Field.Store.YES));
        //页面内容全文
        document.add(new TextField("CONTENT", blog.getContent(), Field.Store.YES));
        //作者
        document.add(new StringField("AUTHOR", blog.getAuthor(), Field.Store.YES));
        //发表时间
        document.add(new LongPoint("DATE", blog.getDate()));
        document.add(new StoredField("DATE", blog.getDate()));
        //标签
        if(blog.getTags() != null){
            for(String tag : blog.getTags()){
                document.add(new StringField("TAG", tag.trim(), Field.Store.YES));
            }
        }
        //文章URL地址
        String url = String.format("https://www.cnblogs.com/%s/p/%s.html",
                blog.getAuthor() != null ? blog.getAuthor() : "", blog.getId());
        document.add(new StringField("URL", url, Field.Store.YES));
        //博文动态信息(BlogStats)
        BlogStats stats = blog.getBlogStats();
        if(stats != null){
            document.add(new IntPoint("VIEW_COUNT", stats.getViewCount()));
            document.add(new StoredField("VIEW_COUNT", stats.getViewCount()));
            document.add(new IntPoint("FEEDBACK_COUNT", stats.getFeedbackCount()));
            document.add(new StoredField("FEEDBACK_COUNT", stats.getFeedbackCount()));
            document.add(new IntPoint("DIGG_COUNT", stats.getDiggCount()));
            document.add(new StoredField("DIGG_COUNT", stats.getDiggCount()));
            document.add(new IntPoint("BURY_COUNT", stats.getBuryCount()));
            document.add(new StoredField("BURY_COUNT", stats.getBuryCount()));
        }
        return document;
    }
}
