package com.example.yuimagesearchmcpserver.dynamic.mapper;

import com.example.yuimagesearchmcpserver.dynamic.eneity.ApiToolConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ApiToolConfigRepository {
    /**
     * 查询所有启用的工具配置
     * @return 启用的工具配置列表
     */
    @Select("SELECT * FROM api_tool_config WHERE enabled = 1")
    List<ApiToolConfig> findAllEnabled();

}
