package com.dyx.yuaiagent.controller;

import com.dyx.yuaiagent.agent.Manus;
import com.dyx.yuaiagent.app.doctorApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController()
@RequestMapping("/ai")
public class AiController {

    @Resource
    private doctorApp doctorApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;
    /**
     * 同步调用应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/app/chat/sync")
    public String doChatWithLoveAppSync(String message,String chatId){

        return doctorApp.doChat(message,chatId);
    }

    /**
     * 1 sse调用应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithAppSSE(String message, String chatId){
        return doctorApp.doChatByStream(message,chatId);
    }

    /**
     *2 sSe调用应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithAppServerSentEvent(String message, String chatId){
        return doctorApp.doChatByStream(message,chatId)
                .map(chunk-> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }




    /**
     * 3 sSe调用应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/app/chat/see_emitter")
    public SseEmitter doChatWithAppServerSentEmitter(String message, String chatId){
        //创建一个超时时间较长的SseEmiter，服务器主动向前端发送消息
        SseEmitter sseEmitter=new SseEmitter(1800000L);
        //获取Flux响应式数据流并且直接通过订阅推送给SseEmitter
        doctorApp.doChatByStream(message,chatId)
                .subscribe(chunk->{//监听流式响应对象
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                },e->{sseEmitter.completeWithError(e);},sseEmitter::complete);
        //返回
        return sseEmitter;
    }


    /**
     * 流使调用智能体
     * @param message
     * @return
     */
    @GetMapping(value = "/manus/chat")
    public SseEmitter doChatWithManus(String message){
        Manus manus = new Manus(allTools, dashscopeChatModel);
        return manus.runStream(message);
    }






}
