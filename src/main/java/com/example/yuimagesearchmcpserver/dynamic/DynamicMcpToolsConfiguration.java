package com.example.yuimagesearchmcpserver.dynamic;

import com.example.yuimagesearchmcpserver.dynamic.DynamicHttpToolCallbackFactory;
import com.example.yuimagesearchmcpserver.dynamic.mapper.ApiToolConfigRepository;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
@Configuration
public class DynamicMcpToolsConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMcpToolsConfiguration.class);

    /**
     * 创建 RestTemplate Bean（如果项目中还没有的话）
     * 如果你的项目中已经配置了 RestTemplate，可以删除这个方法
     */
    @Bean
    @ConditionalOnMissingBean(name = "restTemplate")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 创建动态 HTTP 工具回调工厂
     */
    @Bean
    public DynamicHttpToolCallbackFactory dynamicHttpToolCallbackFactory(ApiToolConfigRepository repository,
                                                                         RestTemplate restTemplate) {
        return new DynamicHttpToolCallbackFactory(repository, restTemplate);
    }

    /**
     * 将动态工具转换为 MCP SyncToolSpecification 并注册到 MCP Server
     *
     * <p>这个方法返回的 List 会被 {@link org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration}
     * 自动收集并注册到 MCP Server。
     */
    @Bean
    public List<SyncToolSpecification> dynamicSyncToolSpecifications(
            DynamicHttpToolCallbackFactory factory) {
        logger.info("Initializing dynamic MCP tools from database...");

        List<ToolCallback> callbacks = factory.loadToolCallbacks();

        if (callbacks.isEmpty()) {
            logger.warn("No dynamic tools found, returning empty list");
            return List.of();
        }

        // 将 Spring AI 的 ToolCallback 转换为 MCP SyncToolSpecification
        List<SyncToolSpecification> specifications = McpToolUtils.toSyncToolSpecification(callbacks);

        logger.info("Successfully registered {} dynamic MCP tools", specifications.size());
        return specifications;
    }
//    /**
//     * 动态 HTTP 工具的 ToolCallbackProvider，供 ChatClient 本地使用
//     *
//     * <p>当 Chat 应用需要直接使用「数据库里配置的 HTTP 工具」（不经过 MCP Server 协议）时，
//     * 注入此 Bean，在 {@code client.prompt().user(...).tools(dynamicHttpToolCallbackProvider).call()}
//     * 中使用即可。用法与 @Tool + MethodToolCallbackProvider 一致。
//     *
//     * <p>若 Chat 应用通过 MCP Client 连接 MCP Server 使用工具，则使用
//     * {@link org.springframework.ai.mcp.SyncMcpToolCallbackProvider} 即可，无需此 Bean。
//     */
//    @Bean
//    public ToolCallbackProvider dynamicHttpToolCallbackProvider(DynamicHttpToolCallbackFactory factory) {
//        List<ToolCallback> callbacks = factory.loadToolCallbacks();
//        return ToolCallbackProvider.from(callbacks);
//    }

}
