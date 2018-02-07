package cn.zichao.jdkLearning.ThreadLocal.core;

public class MyThreadLocalThread extends Thread {

    MyThreadLocal.MyThreadLocalMap threadLocalMap = new MyThreadLocal.MyThreadLocalMap();

    public MyThreadLocalThread(Runnable target) {
        super(target);
    }
}
