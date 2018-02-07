package cn.zichao.jdkLearning.ThreadLocal.core;

import java.lang.ref.*;

public class MyThreadLocal<T> {

    private int index = -1;

    public T get(){
        MyThreadLocalMap map = getMap(Thread.currentThread());
        if (map != null){
            MyThreadLocalMap.Entry entry = map.get(this);
            if (entry != null){
                return (T)entry.value;
            }
        }
        return null;
    }
    public void set(T t){
        MyThreadLocalMap map = getMap(Thread.currentThread());
        if (map != null){
            map.set(this,t);
        }
    }


    public T remove(){


        return null;
    }

    private MyThreadLocalMap getMap(Thread thread) {

        if (thread instanceof MyThreadLocalThread){
            return ((MyThreadLocalThread)thread).threadLocalMap;
        }
        return null;

    }
    private void setIndex(int i){
        this.index = i;
    }











    static class MyThreadLocalMap{



        static class Entry extends WeakReference<MyThreadLocal>{
            Object value;
            public Entry(MyThreadLocal referent,Object value) {
                super(referent);
                this.value = value;
            }

        }
        private MyThreadLocal.MyThreadLocalMap.Entry[] table;
        private  static final int INIT_CAPACITY = 16;
        private int threshold;
        private int size = 0;
        private int currentIndex = 0;
        private int[] indexes;

        public MyThreadLocalMap(){
            //初始化Entry数组
            table = new Entry[INIT_CAPACITY];
            //初始化indexes数组
            indexes = new int[INIT_CAPACITY];
            for (int i = 0;i < INIT_CAPACITY;i++){
                indexes[i] = i;
            }
            //设置压力值
            setThreshold(INIT_CAPACITY);
        }

        private void resize(){
            //获取原有长度
            int oldLen = table.length;
            int newLen = oldLen *  2;
            //重新构建索引
            resizeIndexes(newLen);
            //重新构建entry
            Entry[] newTable = new Entry[newLen];
            for (int i = 0;i < oldLen;i++){
                newTable[i] = table[i];
            }
            //设置新压力值
            setThreshold(newLen);
            table = newTable;
        }

        //重新扩充索引
        private void resizeIndexes(int len){
            int[] newIndex = new int[len];
            int gap = indexes.length - currentIndex;
            //复制之前的
            int i;
            for (i = 0;i < gap;i++){
                newIndex[i] = indexes[currentIndex + i];
            }
            //删除被释放的节点
            int j = 0;
            while (table[j]  != null){
                if (table[j].get() != null){
                    break;
                }
                table[j].value = null;
                table[j] = null;
                size--;
                newIndex[i++] = j++;
            }
            int start = indexes[indexes.length - 1];
            //初始化之后的
            for (;i < len;i++){
                newIndex[i] = ++start;
            }
            indexes = newIndex;
            currentIndex = 0;
        }

        private void setThreshold(int len){
            threshold = len * 3 / 4;
        }
        private int nextIndex() {
            int t = indexes[currentIndex];
            currentIndex++;
            return t;
        }

        private Entry get(MyThreadLocal threadLocal){
            int index = threadLocal.index;
            if (index == -1){
                return  null;
            }
            Entry ret = table[index];
            if (ret != null){
                return ret;
            }
            return null;
        }
        private void set(MyThreadLocal threadLocal,Object t) {
            int index = threadLocal.index;
            if (index == -1){
                threadLocal.setIndex((index = nextIndex()));
            }
            Entry e = table[index];
            if (e == null){
                table[index] = new Entry(threadLocal,t);
            }else {
                e.value = t;
            }
            size++;
            if (size > threshold){
                resize();
            }
        }


    }

}
