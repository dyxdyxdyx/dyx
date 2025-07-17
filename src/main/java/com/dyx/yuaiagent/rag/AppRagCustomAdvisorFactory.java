package com.dyx.yuaiagent.rag;

import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的rag检索增强顾问的工厂
 */
public class AppRagCustomAdvisorFactory {
    /**
     * 创建自定义的rag增强顾问
     * @param vectorStore
     * @param status
     * @return
     */
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore, String status){
        //过滤特定状态的文档
        Filter.Expression expression = new FilterExpressionBuilder().eq("status", status).build();
        //用向量数据库的文档检索器
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .similarityThreshold(0.5)
                .topK(3)//返回文档数量
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)//文档过滤器，指定条件过滤
                .queryAugmenter(AppContextualQueryAugmentFactory.createInstance())//文档（查询）增强器
                .build();
    }






}
