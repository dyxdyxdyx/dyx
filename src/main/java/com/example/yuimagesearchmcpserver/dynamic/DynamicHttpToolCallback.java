package com.example.yuimagesearchmcpserver.dynamic;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.example.yuimagesearchmcpserver.dynamic.eneity.ApiToolConfig;

import org.springframework.lang.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.util.json.schema.JsonSchemaUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
public class DynamicHttpToolCallback implements ToolCallback  {


    private static final Logger logger = LoggerFactory.getLogger(DynamicHttpToolCallback.class);

    private final ToolDefinition toolDefinition;

    private final ApiToolConfig config;

    private final RestTemplate restTemplate;

    /**
     * 创建动态 HTTP 工具回调
     * @param config API 工具配置
     * @param restTemplate HTTP 客户端
     */
    public DynamicHttpToolCallback(ApiToolConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;

        // 构建 ToolDefinition，这个会暴露给 LLM
        this.toolDefinition = DefaultToolDefinition.builder()
                .name(config.getToolName())
                .description(config.getDescription())
                .inputSchema(JsonSchemaUtils.ensureValidInputSchema(config.getInputSchemaJson()))
                .build();

        logger.debug("Created DynamicHttpToolCallback for tool: {}", config.getToolName());
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return this.toolDefinition;
    }
    public String call(String toolInput) {
        return this.call(toolInput, (ToolContext)null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        logger.debug("Calling tool: {} with input: {}", config.getToolName(), toolInput);

        try {
            // 1. 解析 LLM/MCP 传来的 JSON 参数
            Map<String, Object> raw = ModelOptionsUtils.jsonToMap(toolInput);
            Map<String, Object> params = resolveParams(raw);
            logger.info("[DynamicHttp] tool={}, rawKeys={}, resolvedParams={}", config.getToolName(), raw.keySet(), params);

            // 2. 确定 HTTP 方法
            HttpMethod httpMethod = HttpMethod.valueOf(config.getHttpMethod().toUpperCase());

            // 3. 根据方法类型构建请求：GET/HEAD/DELETE 用查询参数，POST/PUT/PATCH 用请求体
            // 使用 RequestEntity<?>：GET/HEAD/DELETE 为 RequestEntity<Void>，POST/PUT/PATCH 为 RequestEntity<String>
            RequestEntity<?> request;
            URI requestUri;

            if (httpMethod == HttpMethod.GET || httpMethod == HttpMethod.HEAD || httpMethod == HttpMethod.DELETE) {
                // GET/HEAD/DELETE：先替换路径参数 {paramName}，其余作为查询参数，无请求体
                String urlWithPathParams = config.getUrl();
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    if (e.getValue() != null && !"".equals(e.getValue())) {
                        String placeholder = "{" + e.getKey() + "}";
                        if (urlWithPathParams.contains(placeholder)) {
                            urlWithPathParams = urlWithPathParams.replace(placeholder, String.valueOf(e.getValue()));
                        }
                    }
                }
                UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(urlWithPathParams);
                String originalUrl = config.getUrl();
                params.forEach((key, value) -> {
                    if (value == null || "".equals(value) || originalUrl.contains("{" + key + "}")) return;
                    String str = value instanceof Map || value instanceof List ? null : String.valueOf(value).trim();
                    if (str != null && !str.isEmpty()) {
                        // 兼容被错误序列化为 "0xe70x8c0xab" 的 UTF-8 中文等，解码回真实字符串
                        str = decodeHexUtf8IfNeeded(str);
                        uriBuilder.queryParam(key, str);
                    }
                });
                requestUri = uriBuilder.build().encode(StandardCharsets.UTF_8).toUri();
                // GET/HEAD/DELETE 无请求体，使用对应方法构建
                if (httpMethod == HttpMethod.GET) {
                    request = RequestEntity.get(requestUri).header("Accept", "application/json").build();
                } else if (httpMethod == HttpMethod.HEAD) {
                    request = RequestEntity.head(requestUri).header("Accept", "application/json").build();
                } else {
                    request = RequestEntity.delete(requestUri).header("Accept", "application/json").build();
                }
            } else {
                // POST/PUT/PATCH：请求体为 JSON
                requestUri = URI.create(config.getUrl());
                String bodyJson = ModelOptionsUtils.toJsonString(params);
                request = RequestEntity.method(httpMethod, requestUri)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .body(bodyJson);
            }

            logger.debug("Sending {} request to: {}", httpMethod, requestUri);

            // 4. 发送 HTTP 请求
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);

            // 5. 检查响应状态
            if (!response.getStatusCode().is2xxSuccessful()) {
                String errorMsg = String.format("HTTP request failed with status %s: %s", response.getStatusCode(),
                        response.getBody());
                logger.error("Tool execution failed: {}", errorMsg);
                throw new ToolExecutionException(this.toolDefinition,
                        new IllegalStateException(errorMsg));
            }

            // 6. 返回响应体给 LLM
            String result = response.getBody() != null ? response.getBody() : "{}";
            logger.debug("Tool execution successful: {}, response length: {}", config.getToolName(),
                    result.length());
            return result;

        } catch (RestClientException ex) {
            logger.error("HTTP request failed for tool: {}", config.getToolName(), ex);
            throw new ToolExecutionException(this.toolDefinition, ex);
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid HTTP method or URL for tool: {}", config.getToolName(), ex);
            throw new ToolExecutionException(this.toolDefinition, ex);
        } catch (Exception ex) {
            logger.error("Unexpected error executing tool: {}", config.getToolName(), ex);
            throw new ToolExecutionException(this.toolDefinition, ex);
        }
    }

    /**
     * 解析实际参数：兼容 MCP/JSON-RPC 多种包裹格式。
     * 支持：{"query":"猫"}、{"arguments":{"query":"猫"}}、{"params":{"arguments":{"query":"猫"}}} 等。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveParams(Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return new LinkedHashMap<>();
        }
        // 先看是否被包在 JSON-RPC 的 params 里
        Object paramsObj = raw.get("params");
        if (paramsObj instanceof Map) {
            raw = (Map<String, Object>) paramsObj;
        }
        for (String key : new String[] { "arguments", "input", "parameters" }) {
            Object args = raw.get(key);
            if (args instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) args);
            }
            if (args instanceof String && org.springframework.util.StringUtils.hasText((String) args)) {
                try {
                    return ModelOptionsUtils.jsonToMap((String) args);
                } catch (Exception e) {
                    logger.debug("Failed to parse {} as JSON", key);
                }
            }
        }
        // 若顶层没有 arguments/input，但已有业务参数（如 query），直接用 raw
        return raw;
    }

    /** 匹配被错误序列化为 "0xe70x8c0xab" 形式的 UTF-8 十六进制串 */
    private static final Pattern HEX_UTF8_PATTERN = Pattern.compile("^(0x[0-9a-fA-F]{2}\\s*)+$");

    /**
     * 若字符串形如 "0xe70x8c0xab"（UTF-8 字节的十六进制），解码为真实字符串（如 "猫"）；
     * 否则原样返回。
     */
    private static String decodeHexUtf8IfNeeded(String value) {
        if (value == null || value.isEmpty()) return value;
        String trimmed = value.trim();
        if (!HEX_UTF8_PATTERN.matcher(trimmed).matches()) return value;
        try {
            String[] parts = trimmed.split("0x");
            List<Byte> bytes = new ArrayList<>();
            for (String part : parts) {
                String hex = part.trim();
                if (hex.isEmpty()) continue;
                if (hex.length() >= 2) {
                    bytes.add((byte) Integer.parseInt(hex.substring(0, 2), 16));
                }
            }
            byte[] arr = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); i++) arr[i] = bytes.get(i);
            return new String(arr, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.debug("Failed to decode hex UTF-8 string: {}", value, e);
            return value;
        }
    }
}
