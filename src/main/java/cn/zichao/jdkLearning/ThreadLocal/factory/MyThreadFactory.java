package cn.zichao.jdkLearning.ThreadLocal.factory;

import cn.zichao.jdkLearning.ThreadLocal.core.MyThreadLocalThread;

import java.util.concurrent.ThreadFactory;

public class MyThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
        return new MyThreadLocalThread(r);
    }
}
