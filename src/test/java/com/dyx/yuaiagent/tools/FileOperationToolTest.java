package com.dyx.yuaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FileOperationToolTest {

    @Test
    void readFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String filename="123.txt";
        String s = fileOperationTool.readFile(filename);
        System.out.println(s);
    }

    @Test
    void writeFile() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        String filename="123.txt";
        String content="132";
        String s = fileOperationTool.writeFile(filename, content);
        System.out.println(s);

    }
}