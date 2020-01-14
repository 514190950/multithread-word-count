package com.github.hcsp.multithread;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

// CountDownLatch
public class MultiThreadWordCount2 {

    // 使用threadNum个线程，并发统计文件中各单词的数量
    public static Map<String, Integer> count(int threadNum, List<File> files) throws InterruptedException {
        Map<String, Integer> resultMap = new ConcurrentHashMap<>();
        CountDownLatch countDownLatch = new CountDownLatch(files.size());
        for (File file : files) {
            new Thread(() -> {
                try {
                    Map<String, Integer> map = Common.countOneFile(file);
                    synchronized (MultiThreadWordCount2.class) {
                        Common.mergeToMap(resultMap, map);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await();
        return resultMap;
    }
}
