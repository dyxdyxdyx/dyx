package com.yupi.yuaiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用作创建实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具，springAI工具调用的对象就是 ToolCallback对象
    private final ToolCallback[] availableTools;

    // 保存了工具调用信息的响应结果（要调用哪些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用springAi内置的工具调用机制，自己维护上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)//不让springAi代理
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     *
     * @return 是否需要执行行动，true表示需要执行，false表示不需要执行
     * 要么执行工具，要么不执行工具
     */
    @Override
    public boolean think() {
        try {
            //1校验提示词，把上一步的提示词拼接到用户提示词中
            if (StrUtil.isNotBlank(getNextStepPrompt())) {
                //发送给ai
                UserMessage userMessage = new UserMessage(getNextStepPrompt());
                getMessageList().add(userMessage);
            }

            List<Message> messageList = getMessageList();
            //this.chatOptions决定了不会托管工具调用，而是自己实现工具调用
            Prompt prompt = new Prompt(messageList, this.chatOptions);

            //2 调用Ai大模型，获取AI工具调用的结果
            ChatResponse chatResponse = getChatClient()
                    .prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            //记录响应，用于等下执行Act
            this.toolCallChatResponse = chatResponse;
            //异常处理
            //3，解析工具调用结果，获取要调用的工具
            //助手信息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            //输出提示信息，ai的思考结果，就是ai想要调用哪些工具
            String result = assistantMessage.getText();
            log.info(getName() + "的思考" + result);
            log.info(getName() + "选这了" + toolCallList.size() + "个工具来使用");

            String toolCallInfo = toolCallList.stream().map(toolCall -> {
                return String.format("工具名称：%s,参数:%s", toolCall.name(), toolCall.arguments());
            }).collect(Collectors.joining("\n"));

            log.info(toolCallInfo);

            if (toolCallInfo.isEmpty()) {
                //只有不调用工具的时候，，把助手消息添加到上下文中
                getMessageList().add(assistantMessage);
                return false;
            } else {
                //需要调用工具的时候，无需记录助手信息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "思考出现了问题" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        // 当前工具调用的结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> "doTerminate".equals(response.name()));
        if (terminateToolCalled) {
            setState(AgentState.FINISHED);
        }
        log.info(results);
        return results;

    }

}
