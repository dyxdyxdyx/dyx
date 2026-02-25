package com.example.yuimagesearchmcpserver.dynamic;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.example.yuimagesearchmcpserver.dynamic.eneity.ApiToolConfig;
import com.example.yuimagesearchmcpserver.dynamic.mapper.ApiToolConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
public class DynamicHttpToolCallbackFactory {
    private static final Logger logger = LoggerFactory.getLogger(DynamicHttpToolCallbackFactory.class);

    private final ApiToolConfigRepository repository;

    private final RestTemplate restTemplate;

    /**
     * 创建工厂实例
     * @param repository API 工具配置仓库
     * @param restTemplate HTTP 客户端
     */
    public DynamicHttpToolCallbackFactory(ApiToolConfigRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    /**
     * 从数据库加载所有启用的工具配置，并创建对应的 ToolCallback 列表
     * @return ToolCallback 列表
     */
    public List<ToolCallback> loadToolCallbacks() {
        logger.info("Loading dynamic HTTP tool callbacks from database...");

        List<ApiToolConfig> configs = repository.findAllEnabled();

        if (CollectionUtils.isEmpty(configs)) {
            logger.warn("No enabled API tool configurations found in database");
            return List.of();
        }

        logger.info("Found {} enabled API tool configurations", configs.size());

        List<ToolCallback> callbacks = configs.stream()
                .map(config -> {
                    try {
                        // 验证配置
                        validateConfig(config);
                        return new DynamicHttpToolCallback(config, restTemplate);
                    }
                    catch (Exception ex) {
                        logger.error("Failed to create ToolCallback for tool: {}", config.getToolName(), ex);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        logger.info("Successfully created {} ToolCallback instances", callbacks.size());
        return callbacks;
    }

    /**
     * 验证配置是否有效
     */
    private void validateConfig(ApiToolConfig config) {
        if (!org.springframework.util.StringUtils.hasText(config.getToolName())) {
            throw new IllegalArgumentException("toolName cannot be empty");
        }
        if (!org.springframework.util.StringUtils.hasText(config.getUrl())) {
            throw new IllegalArgumentException("url cannot be empty");
        }
        if (!org.springframework.util.StringUtils.hasText(config.getHttpMethod())) {
            throw new IllegalArgumentException("httpMethod cannot be empty");
        }
        if (!org.springframework.util.StringUtils.hasText(config.getInputSchemaJson())) {
            throw new IllegalArgumentException("inputSchemaJson cannot be empty");
        }
        // 验证 HTTP 方法是否有效
        try {
            org.springframework.http.HttpMethod.valueOf(config.getHttpMethod().toUpperCase());
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid httpMethod: " + config.getHttpMethod(), ex);
        }
    }
}
