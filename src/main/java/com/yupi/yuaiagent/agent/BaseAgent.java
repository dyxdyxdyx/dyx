package com.yupi.yuaiagent.agent;

import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 *
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示
    private String systemPrompt;
    // 中间提示词
    private String nextStepPrompt;

    // 代理状态，默认为空闲状态
    private AgentState state = AgentState.IDLE;

    // 执行控制
    private int maxSteps = 10;//步骤控制的最大数
    private int currentStep = 0;//当前步骤

    // LLM
    private ChatClient chatClient;

    // Memory（需要自主维护会话上下文），需要让哪些会话添加到会话中，哪些不添加
    private List<Message> messageList = new ArrayList<>();

    /**
     * 运行代理
     * 调用智能体的方法
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 更改状态
        state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));//这里调用springAi封装的userMessage封装一下提示词
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step " + stepNumber + "/" + maxSteps);
                // 单步执行
                String stepResult = step();//步骤结果
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("Error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 清理资源
            this.cleanup();
        }
    }





    /**
     * 运行代理 sse流式输出
     * 调用智能体的方法
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStream(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L);
        //使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(
                ()->{
                    try {
                        if (this.state != AgentState.IDLE) {
                            sseEmitter.send("错误；无法从状态获取代理："+this.state);
                            sseEmitter.complete();
                            return;
                        }
                        if (StringUtil.isBlank(userPrompt)) {
                            sseEmitter.send("不能使用空提示词运行代理：");
                            sseEmitter.complete();
                            return;
                        }
                    } catch (Exception e) {
                        sseEmitter.completeWithError(e);
                    }
                    // 更改状态
                    state = AgentState.RUNNING;
                    // 记录消息上下文
                    messageList.add(new UserMessage(userPrompt));//这里调用springAi封装的userMessage封装一下提示词
                    // 保存结果列表
                    List<String> results = new ArrayList<>();
                    try {
                        for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                            int stepNumber = i + 1;
                            currentStep = stepNumber;
                            log.info("Executing step " + stepNumber + "/" + maxSteps);
                            // 单步执行
                            String stepResult = step();//步骤结果
                            String result = "Step " + stepNumber + ": " + stepResult;
                            results.add(result);
                            //加一个消息推送，输出每一步的消息结果推给sseEmitter
                            sseEmitter.send(result);
                        }
                        // 检查是否超出步骤限制
                        if (currentStep >= maxSteps) {
                            state = AgentState.FINISHED;
                            results.add("Terminated: Reached max steps (" + maxSteps + ")");
                            sseEmitter.send("执行结束，达到最大步骤"+maxSteps);

                        }
                        //正常完成
                        sseEmitter.complete();
                    } catch (Exception e) {
                        state = AgentState.ERROR;
                        log.error("Error executing agent", e);
                        try {
                            sseEmitter.send("执行错误"+e.getMessage());
                            sseEmitter.complete();
                        } catch (IOException ex) {
                            sseEmitter.completeWithError(ex);
                        }
                    } finally {
                        // 清理资源
                        this.cleanup();
                    }
                }
        );
        //设置超时回调
        sseEmitter.onTimeout(()->{
            this.state=AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });
        //设置完成回调
        sseEmitter.onCompletion(()->{
            if (this.state==AgentState.RUNNING){
                this.state=AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });
        return sseEmitter;
    }
    /**
     * 执行单个步骤，交给字类去实现
     *
     * @return 步骤执行结果
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }
}
