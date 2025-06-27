package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.advisor.ReReadingAdvisor;
import com.yupi.yuaiagent.chatmemory.FileBaseChatMemory;
import com.yupi.yuaiagent.rag.AppRagCustomAdvisorFactory;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class doctorApp {
    private final ChatClient client;
    private static final String SYSTEM_PROMPT="扮演一位资深的全科医生，需要为患者解答疑惑，并且给出治疗方案";

    /**
     * 初始化ai客户端
     * //是 Spring 根据你在 application.yml 中的配置自动创建的 Bean。
     * 使用哪个模型（qwen-plus）
     * 使用哪个 API Key（AI_DASHSCOPE_API_KEY）
     * 请求参数、超时设置等
     * @param dashscopeChatModel
     */
    public doctorApp(ChatModel dashscopeChatModel)
    {
        //初始化基于内存的对话记以
        ChatMemory chatMemory=new InMemoryChatMemory();
//        String fileDir=System.getProperty("user.dir")+"/chat-memory";
        client=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
//                        new ReReadingAdvisor()
                )
                .build();
    }
    /**
     * 支持多轮会话
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message,String chatId)
    {
        ChatResponse chatResponse = client.prompt().user(message).advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        return content;
    }



    /**
     * 支持多轮会话，异步的
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message,String chatId)
    {
        Flux<String> content1 = client.prompt().user(message).advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();

        content1.subscribe(content->log.info("content:{}",content));
        return content1;
    }


    record LoveReport(String title, List<String> suggestions)
    {

    }
    /**
     * AI医疗报告功能（）实战结构化输出
     * @param message
     * @param chatId
     * @return
     */
    public LoveReport doChatWithReport(String message,String chatId)
    {
        LoveReport loveReport = client.prompt()
                .system(SYSTEM_PROMPT+"每次对话后都要生成诊断结果，标题为{用户名}的诊断结果，内容为建议列表")
                .user(message).advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport:{}",loveReport);
        return loveReport;
    }


    /**
     * ai恋爱知识库问答功能
     * rag知识库进行对话
     */
    @Resource
    private VectorStore AppVectorStore;

//    @Resource
//    private VectorStore pgVectorVectorStore;
    @Resource
    private Advisor AppRagCloudAdvisor;
    @Resource
    private QueryRewriter queryRewriter;
    public String doChatWithRag(String message,String chatId){

        //使用查询重写
        String rewriteMessage = queryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = client.prompt()
                .user(rewriteMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                //开启日志
                .advisors(new MyLoggerAdvisor())
              //  .advisors(new QuestionAnswerAdvisor(AppVectorStore))
                //应用rag，基于云知识库增强
               // .advisors(AppRagCloudAdvisor)
                //应用rag检索增强服务，基于pgvector
               // .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                //应用自定义的rag查询增强器
                .advisors(AppRagCustomAdvisorFactory.createLoveAppRagCustomAdvisor(
                        AppVectorStore,"老年"
                ))
                .call()
                .chatResponse();

        String content = chatResponse.getResult().getOutput().getText();
        log.info("content:{}",content);
        return content;
    }


    /**
     * 调用工具能力
     */

    @Resource
    private ToolCallback[] allTools;
    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = client
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 启动的时候，会自动读取刚刚写的mcp.json文件，然后找到所有的工具，自动注册到工具提供者类上
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public String doChatWithMcp(String message, String chatId) {
        ChatResponse response = client
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }




}
