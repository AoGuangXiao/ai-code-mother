package com.agx.aicodemother.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.bilibili.com/";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }
}