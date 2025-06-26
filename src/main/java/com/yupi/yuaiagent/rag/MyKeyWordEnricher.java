package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于ai的文档元信息增强器，为文档增加元信息
 */
@Component
public class MyKeyWordEnricher {

    @Resource
    private ChatModel dashscopeChatModel;

    public List<Document> enrichDocuments(List<Document> documents){
        //生成5个关键词
        KeywordMetadataEnricher keywordMetadataEnricher =
                new KeywordMetadataEnricher(dashscopeChatModel, 5);

        return keywordMetadataEnricher.apply(documents);

    }


}
