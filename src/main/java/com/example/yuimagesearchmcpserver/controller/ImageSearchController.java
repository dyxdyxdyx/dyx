package com.example.yuimagesearchmcpserver.controller;

import com.example.yuimagesearchmcpserver.tools.ImageSearchTool;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/image")
public class ImageSearchController {

    private final ImageSearchTool imageSearchTool;

    public ImageSearchController(ImageSearchTool imageSearchTool) {
        this.imageSearchTool = imageSearchTool;
    }

    /**
     * 根据关键词搜索图片，返回逗号分隔的图片 URL 字符串
     * GET /api/image/search?query=关键词
     */
    @GetMapping("/search")
    public String searchImage(@RequestParam String query) {
        return imageSearchTool.searchImage(query);
    }

    /**
     * 根据关键词搜索图片，返回中等尺寸图片 URL 列表
     * GET /api/image/search/list?query=关键词
     */
    @GetMapping("/search/list")
    public List<String> searchMediumImages(@RequestParam String query) {
        return imageSearchTool.searchMediumImages(query);
    }
}
