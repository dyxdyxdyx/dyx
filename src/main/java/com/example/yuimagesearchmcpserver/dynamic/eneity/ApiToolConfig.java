package com.example.yuimagesearchmcpserver.dynamic.eneity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName(value = "api_tool_config",autoResultMap = true)
@Data
public class ApiToolConfig {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具名称（暴露给 LLM 的工具名，必须唯一且稳定）
     * 例如: "getUserInfo", "createOrder"
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 工具描述（给 LLM 看的，说明这个工具是做什么的）
     * 例如: "根据用户ID获取用户详细信息"
     */
    @TableField("description")
    private String description;

    /**
     * HTTP 方法: GET, POST, PUT, DELETE, PATCH
     */
    @TableField("http_method")
    private String httpMethod;

    /**
     * 完整的 HTTP URL
     * 例如: "https://api.example.com/user/detail"
     * 或者: "http://internal-service:8080/api/order/create"
     */
    @TableField("url")
    private String url;

    /**
     * 入参 JSON Schema（JSON 字符串格式）
     * 这个 Schema 会暴露给 LLM，告诉它这个工具需要什么参数
     *
     * 示例:
     * <pre>
     * {
     *   "type": "object",
     *   "properties": {
     *     "userId": {
     *       "type": "string",
     *       "description": "用户ID"
     *     },
     *     "includeDetails": {
     *       "type": "boolean",
     *       "description": "是否包含详细信息"
     *     }
     *   },
     *   "required": ["userId"]
     * }
     * </pre>
     */
    @TableField("input_schema_json")
    private String inputSchemaJson;

    /**
     * 固定的 HTTP 请求头（JSON 字符串格式，可选）
     * 例如: {"Authorization": "Bearer token", "X-Client-Version": "1.0"}
     */
    @TableField("headers_json")
    private String headersJson;

    /**
     * 请求超时时间（毫秒），默认 5000
     */
    @TableField("timeout_millis")
    private Integer timeoutMillis;

    /**
     * 是否启用此工具
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private java.time.LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private java.time.LocalDateTime updateTime;

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getInputSchemaJson() {
        return inputSchemaJson;
    }

    public void setInputSchemaJson(String inputSchemaJson) {
        this.inputSchemaJson = inputSchemaJson;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public Integer getTimeoutMillis() {
        return timeoutMillis != null ? timeoutMillis : 5000;
    }

    public void setTimeoutMillis(Integer timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public Boolean getEnabled() {
        return enabled != null ? enabled : true;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public java.time.LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.time.LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public java.time.LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(java.time.LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "ApiToolConfig{" + "id=" + id + ", toolName='" + toolName + '\'' + ", description='" + description
                + '\'' + ", httpMethod='" + httpMethod + '\'' + ", url='" + url + '\'' + ", enabled=" + enabled + '}';
    }
}
