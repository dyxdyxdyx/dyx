package com.yupi.yuaiagent;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Types;
import java.util.Collections;

@SpringBootTest
class YuAiAgentApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
// 代码生成
    void generatorCode(){
        String dbUrl="jdbc:mysql://127.0.0.1:3306/a_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "123";
        // 基础代码生成的路径
        final String basedir = "D:\\code\\yu-ai-agent\\src\\main\\java";
        // Mapper生成的路径
        final String basedirMapper = "D:\\code\\yu-ai-agent\\src\\main\\resources\\mapper";
        FastAutoGenerator.create(dbUrl, username, password)
                .globalConfig(builder -> {
                    builder.author("andy") // 设置作者
                            .enableSwagger() // 开启 swagger 模式
                            .outputDir(basedir); // 指定输出目录
                })
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.SMALLINT) {
                                // 自定义类型转换
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .packageConfig(builder ->
                        builder.parent("com.yupi.yuaiagent") // 设置父包名
                                .moduleName("ai") // 设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, basedirMapper)) // 设置mapperXml生成路径
                )
                .strategyConfig(builder ->
                        builder.addInclude("ai_chat_message","ai_chat_session") // 设置需要生成的表名
                                .addTablePrefix("t_", "c_") // 设置过滤表前缀
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }

}
