package com.yupi.yuaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 向量数据库配置（初始化基于内存的向量数据库Bean）
 */
@Configuration
public class AppVectorStoreConfig {

    @Resource
    private AppDocumentLoader appDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Resource
    private MyKeyWordEnricher myKeyWordEnricher;

    @Bean
    VectorStore AppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        /**
         * 这段代码的作用是：使用 DashScope 的嵌入模型，将 Markdown 格式的医学知识文档进行向量化，
         * 并存储到本地向量数据库中，以便后续对话过程中实现基于语义的内容检索和增强回答。
         */
        SimpleVectorStore simpleVectorStore = SimpleVectorStore
                .builder(dashscopeEmbeddingModel)
                .build();
        //加载文档
        List<Document> documents = appDocumentLoader.loadMarkdowns();
//        //自主切分文档
//        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);
        //生成5个关键词
        List<Document> enrichDocuments = myKeyWordEnricher.enrichDocuments(documents);
        simpleVectorStore.add(enrichDocuments);

        return simpleVectorStore;
    }






}
