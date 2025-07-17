package com.dyx.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AppDocumentLoaderTest {

    @Resource
    private AppDocumentLoader appDocumentLoader;

    @Test
    void loadMarkdowns() {
        appDocumentLoader.loadMarkdowns();
    }
}