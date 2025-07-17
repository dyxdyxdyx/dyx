package com.dyx.yuaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class MultiQueryExpanderDemoTest {


    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;


    @Test
    void expand() {

        List<Query> expand = multiQueryExpanderDemo.expand("失眠了？我该怎么办？");
        System.out.println(expand);


    }
}