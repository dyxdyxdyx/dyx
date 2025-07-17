package com.dyx.yuaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ManusTest {

    @Resource
    private Manus manus;

    @Test
    public void run(){

        String userPrompt = """  
                我的另一半居住在上海静安区，请帮我找到 5 公里内合适的医院地点，  
                并结合一些网络知识，制定一份详细的就医计划，  
                并以 PDF 格式输出""";
        String answer = manus.run(userPrompt);
        System.out.println(answer
        );

    }



}


