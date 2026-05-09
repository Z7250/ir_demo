package cn.edu.bistu.cs.ir.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 面向新浪博客的模型类
 * @author chenruoyu
 */
@Getter
@Setter
public class Blog {

    /**
     * 页面的唯一ID
     */
    private String id;

    /**
     * 标题
     */
    private String title;

    /**
     * 日期
     */
    private long date;

    /**
     * 正文内容
     */
    private String content;

    /**
     * 作者
     */
    private String author;

    /**
     * 标签
     */
    private List<String> tags;

    //阅读数
    private int view;

    //评论数
    private int comment;

    //推荐数
    private int digg;

    //反对数
    private int bury;

    /**
     * 动态获取的博文信息
     */
    private BlogStats blogStats;

}
