package com.sohu.kurento.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jingbiaowang on 2015/12/24.
 *
 * 单例线程池。
 *
 * 获取单例。
 * 允许设置基本线程数，最大线程数，生命时间，...
 *
 * 符合有界队列。
 */
public class SinglExecterPool implements Executor{

    private static SinglExecterPool instance;

    private static ThreadPoolExecutor execterPool;

    private ExecutorService executorService;

    private static int BASE_NUM = 3;

    private static int MAX_NUM = 5;

    private static long KEEPALIVETIME = 10l;

    public static SinglExecterPool getIntance() {

        if (execterPool == null) {
            instance = new SinglExecterPool();
        }

        return instance;
    }

    private SinglExecterPool() {

        BlockingQueue<Runnable> dqueue = new ArrayBlockingQueue<Runnable>(20);
        execterPool = new ThreadPoolExecutor(BASE_NUM, MAX_NUM, KEEPALIVETIME, TimeUnit.SECONDS, dqueue);
    }

    @Override
    public void execute(Runnable command) {

        execterPool.execute(command);
    }

    public void shutdown(){
        execterPool.shutdown();
        execterPool = null;
    }
}
