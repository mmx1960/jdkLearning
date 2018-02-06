package cn.zichao.jdkLearning.ThreadLocal;

import java.util.concurrent.*;

public class TestThreadLocal {
    public static ThreadLocal<String> local = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        local.set("1111");

        ExecutorService singleThreadPool = new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024), new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(()->local.set("2222"));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()->local.set("3333"));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()->local.set("4444"));
        Thread.sleep(10000);
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.shutdown();
        Thread.sleep(1000);
        System.out.println("主线程："+local.get());
    }
}
