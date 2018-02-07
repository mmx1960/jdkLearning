package cn.zichao.jdkLearning.ThreadLocal.demo;

import cn.zichao.jdkLearning.ThreadLocal.core.MyThreadLocal;
import cn.zichao.jdkLearning.ThreadLocal.factory.MyThreadFactory;

import java.util.concurrent.*;

public class TestThreadLocal {
    public static MyThreadLocal<String> local = new MyThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        local.set("1111");

        ExecutorService singleThreadPool = new ThreadPoolExecutor(5, 10,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024),new MyThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        singleThreadPool.execute(()->{
                    for (int i = 0; i < 100; i++) {
                        MyThreadLocal<String> local = new MyThreadLocal<>();
                        local.set("hello"+i);
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < 100; i++) {
                        MyThreadLocal<String> local = new MyThreadLocal<>();
                        local.set("hello"+i);
                    }
                }
                );
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()->local.set("3333"));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()->local.set("4444"));
//        Thread.sleep(10000);
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
//        singleThreadPool.execute(()-> System.out.println(local.get()));
        singleThreadPool.shutdown();
        Thread.sleep(1000);
        System.out.println("主线程："+local.get());
    }
}
