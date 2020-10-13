package com.github.hcsp.multithread;

import com.google.common.collect.Lists;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/****
 * 将所有文件分发给对应线程
 * 每个线程统计自己的数量 (在每个线程中合并)
 * 使用Runnable
 *
 * 使用 CyclicBarrier实现唤醒  (当所有线程都汇总到reduce中 唤醒主线程)
 **/

public class MultiThreadWordCount3 {
    static final Map<String, Integer> REDUCE = new ConcurrentHashMap<>(16);

    static ThreadPoolExecutor executorService;

    static CyclicBarrier cyclicBarrier;


    // 使用threadNum个线程，并发统计文件中各单词的数量
    public static Map<String, Integer> count(int threadNum, List<File> files) throws ExecutionException,
            InterruptedException, BrokenBarrierException {
        executorService = new ThreadPoolExecutor(
                threadNum,
                threadNum,
                10,
                TimeUnit.MINUTES,
                new ArrayBlockingQueue<>(1024),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        // 这里偷懒一下  平均分成10份
        List<List<File>> fileBox = Lists.partition(files, 1);
        cyclicBarrier = new CyclicBarrier(fileBox.size() + 1);
        for (List<File> box : fileBox) {
            executorService.execute(new Executor(box));
        }
        cyclicBarrier.await();
        return REDUCE;
    }


    public static class Executor implements Runnable {
        final List<File> files;

        public Executor(List<File> files) {
            this.files = files;
        }

        @Override
        public void run() {
            Map<String, Integer> countMap = files.stream()
                    .map(FileUtils::readLines)
                    .filter(Objects::nonNull) // 切割成多行
                    .flatMap(Collection::stream)   // 每行独立
                    .map(FileUtils::splitLineToWords)  //分词
                    .flatMap(Collection::stream)
                    .collect(Collectors.groupingBy(x -> x, Collectors.summingInt(x -> 1))); // 计算数量
            synchronized (REDUCE) {
                countMap.forEach((word, count) -> REDUCE.merge(word, count, (a, b) -> b + a));
            }
            try {
                cyclicBarrier.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

}
