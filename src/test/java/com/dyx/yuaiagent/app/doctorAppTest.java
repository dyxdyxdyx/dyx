package com.dyx.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class doctorAppTest {
    @Resource
    private doctorApp app;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        String message="你好，我是鱼皮";
        String answer=app.doChat(message,chatId);
        System.out.println(answer);
        message="我有两个痔疮，怎么解决？";
        answer=app.doChat(message,chatId);
        System.out.println(answer);
        message="我叫什么名字？";
        answer=app.doChat(message,chatId);
        System.out.println(answer);

    }

    @Test
    void doChatWithReport(){
        String chatId = UUID.randomUUID().toString();
        String message="你好，我是鱼皮，我有两个痔疮，怎么解决？";
        doctorApp.LoveReport loveReport = app.doChatWithReport(message, chatId);

        System.out.println(loveReport);
    }


    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message="你好，我是鱼皮，我经常久坐办公室，如何预防颈椎病？";
        String answer = app.doChatWithRag(message, chatId);

        System.out.println(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带去上海奉贤区看病，推荐几个医院？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近新冠病毒又阳了，看看其他网站的人们是怎么解决矛盾的？");


        // 测试文件操作：保存用户档案
        testMessage("保存我的医学档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘健康养生’PDF，包含吃、喝，玩，乐");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = app.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "我居住在上海静安区，请帮我找到 5 公里内合适的医院";
        String answer =  app.doChatWithMcp(message, chatId);
        System.out.println(answer);
        // 测试图片搜索 MCP
//        String message = "帮我搜索一些医院的图片";
//        String answer = app.doChatWithMcp(message, chatId);
//        Assertions.assertNotNull(answer);
    }


}