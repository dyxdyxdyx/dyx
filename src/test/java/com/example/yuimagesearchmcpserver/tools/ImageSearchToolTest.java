package com.example.yuimagesearchmcpserver.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ImageSearchToolTest {
        @Resource
        private ImageSearchTool imageSearchTool;

        @Test
        void searchImage() {
            String result = imageSearchTool.searchImage("computer");
            System.out.println(result);
            Assertions.assertNotNull(result);
        }


}