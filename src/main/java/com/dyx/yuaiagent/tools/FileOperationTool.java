package com.dyx.yuaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.dyx.yuaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 文件操作工具类（提供文件读写功能）
 */
public class FileOperationTool {
    private final String FILE_DIR= FileConstant.FILE_SAVE_DIR+"/file";
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName){
        String filepath=FILE_DIR+"/"+fileName;
        try {
            return FileUtil.readUtf8String(filepath);
        }catch (Exception e){
            return "Error reading file"+e.getMessage();
        }
    }
    @Tool(description = "write content from a file")
    public String writeFile(@ToolParam(description = "Name of a file to read") String fileName,
                            @ToolParam(description = "Content to write to the file") String content){
        String filepath=FILE_DIR+"/"+fileName;
        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filepath);
            return "File written successfully to: " + filepath;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}