package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.print.Doc;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档加载器
 */
@Component
@Slf4j
public class AppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;//spring内置的一个资源解析类
    /**
     * 加载多个文档
     * 构造器注入bean对象， spring自动的注入该依赖
     */
    public AppDocumentLoader(ResourcePatternResolver resourcePatternResolver){
        this.resourcePatternResolver=resourcePatternResolver;
    }
    /**
     * 加载 之后的文档要转换为document文档，加载多篇markdown文档
     */
    public List<Document> loadMarkdowns(){
        //Document 是 Spring AI 中表示文档的基本单位，通常包含文本内容和元数据。
        ArrayList<Document> documents = new ArrayList<>();
        //加载多篇markment文档
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();//获取当前的文件名
                String status = filename.substring(filename.length() - 6, filename.length() - 4);
                //构建一个 MarkdownDocumentReaderConfig 配置对象：
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("status",status)
                        .build();
                //创建一个 MarkdownDocumentReader 实例(spring还有其他的格式的DocumentReader)，传入当前的 resource 和配置 config。
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                //调用 reader.get() 解析当前 Markdown 文件，返回一组 Document；
               documents.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("文档加载失败",e);
        }
        return documents;
    }
}
