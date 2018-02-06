# ThreadLocal

ThreadLocal 用起来简单，概念也很简单，就是存储线程内共享变量，在同一个线程里保持可见性。

为什么要分析这么简单的东西呢？

1. 是为了锻炼自己的源码功底，对技术不能保持只会用而不懂其原理的态度。
2. 是为了后续深入Thread类做个基础准备。

## 操作接口

### 公共接口

我把ThreadLocal比作一个工具类，分析一个工具类首先要从他的公共接口看起，即对外提供了哪些方法。

从ThreadLocal层面来看，主要分成了三个操作

- get
- set
- remove

#### get操作

顾名思义，就是从当前线程中获取这个ThreadLocal对象维护的数据，由此我们可以看到，一个ThreadLocal对象内，只维护一种类型的数据，可以是Map，也可以是List，还可以是某个自定义类型的对象。这个他并不关心。

#### set操作

和上面一样，由于同时只有一个对象，所以每次set前，肯定需要先执行get操作的，不然就失去了ThreadLocal设计的本意。

#### remove操作

从线程中移除掉这个ThreadLocal存储的内容。

大致先介绍下，后面再讨论实现方式。

### 数据结构

单独看ThreadLocal十分简单，但是其内部定义了一个静态内部类ThreadLocalMap，同时这个类里面也定义了一个Entry，作为数据实际的存储。

- ThreadLocalMap
- Entry

Map 和 jdk里的Map不太一样，但又有共同之处，支持get，set操作，内部用一个Entry数组作为存储结构，key是ThreadLocal对象本身，value是用户自定义的值

### 核心源码

在这里说下细节，我把源码核心提取为几个部分。

1. 获取当前线程中的map(保证了一个线程一份数据)
2. key/value构建(构造Entry对象)
3. key/value寻址(从map中获取value的方法，这里不同与jdk的hashmap)
4. 处理map中被清理/GC的节点


#### 获取当前线程中的map

这个很简单，我们看代码:

```java
    //简化了，实际封装了一个函数
    Thread.currentThread().threadLocals;
```

Thread实例内部会维护一个ThreadLocalMap类型的threadLocals,这里先拿到他当前的线程，然后直接获取就行。

#### key/value构建

map的内部存储用的是一个Entry数组，这个Entry并不是由一个简单的键值对实现，而是WeakReference的一个子类，key(在这里也就是ThreadLocal对象本身)通过父类的构造方法的参数传入，作为一个成员存储在对象里(可以通过get方法获取)，value则就是子类的成员，可以直接获取。

```java
static class Entry extends WeakReference<ThreadLocal> {
            /** The value associated with this ThreadLocal. */
            Object value;
            Entry(ThreadLocal k, Object v) {
                super(k);
                value = v;
            }
        }
```

此外，我简单说下rehash，同hashmap一样，这个map的内部结构也是数组，这就意味着，当容量增大到一定程度时，需要重现分配空间，然后对原有的节点做rehash，当然，这个rehash很简单，就是分配一个原有数组大小两倍的新数组，然后把原有数组的每个节点的索引，rehash后重新放入新的数组中。

```java
private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            setThreshold(newLen);
            size = count;
            table = newTab;
        }
```

#### key/value寻址

现在说一个很重要的操作，也就是我怎么获取我这个ThreadLocal所在的位置，不仅是get中，set的时候也需要用到，先要找到实际的位置。
可能大家会想到，直接hash一下，然后索引不就行了么。的确是这样，但是如果两个对象hash到同一个位置怎么办呢？hashmap的实现中，用了链表/红黑树，这个就比较简单了，直接往后递增索引，直到碰到null，说明要找的节点不存在。

截取了一小段set中寻址源码

```java
    int i = key.threadLocalHashCode & (len-1);

            for (Entry e = tab[i];
                 e != null;
                 e = tab[i = nextIndex(i, len)]) {
                ThreadLocal k = e.get();

                if (k == key) {
                    e.value = value;
                    return;
                }

                if (k == null) {
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }
```

#### 处理map中被清理/GC的节点

首先要思考一个问题，什么是被清理，为什么会被清理，我们发现，在找到一个不为空的节点，但是它的key为空，说明被这个节点被清理了。

为什么会这样呢？原因是某个ThreadLocal成员长时间不用，被GC清理掉了，可能你们会问，我每个线程都不同，为什么会有这种情况发生呢，一种是，可能这个线程运行时间很长(比如说主线程)，还有就是我们使用线程一般都是用线程池创建的，尤其是在web应用里，多次请求所分配的线程，很可能是同一个。

还有个疑问，为什么会被回收呢，答案就在上面创建那里，我们的Entry其实是一个WeakReference，这个实现只保持一个弱引用，实际上会被GC掉的。你也许会说，那直接key/value存好啦，用什么弱引用呢？这其实是它做的一种优化，在明确这个值不用的时候，我们可以通过处理这些被GC掉的节点，节约空间。

```java
    // expunge entry at staleSlot
    tab[staleSlot].value = null;
    tab[staleSlot] = null;
    size--;
```

但是仅仅这样是不够的，想象一个场景：比方说A节点 hash到的索引是i,但是i已经有值了，所以最后储存的位置是i+3。中间隔了2个节点，假如这时候i+1被回收了。那么如果我们在查询的时候，把i+1清空，那么i+1就为null，这时候索引还没走到i+3，已经判断空返回找不到了。这样我的A节点就平白无故被吃掉了。。。。

不知道我有没有描述清楚，我们直接来看代码，这里涉及到一个经典的算法，不过不在我们这次的重点，喜欢的同学可以直接去看源码，这里简单的说明下，思路就是合并，从空节点后面开始，如果不为空节点且hash索引和实际的索引不一样，那么就把实际节点值放到从hash索引的位置开始第一个为空的节点上,，直到遇到空节点。

```java
for (i = nextIndex(staleSlot, len);
        (e = tab[i]) != null;
        i = nextIndex(i, len)) {
        ThreadLocal k = e.get();
        if (k == null) {
            e.value = null;
            tab[i] = null;
            size--;
        } else {
            int h = k.threadLocalHashCode & (len - 1);
            if (h != i) {
                tab[i] = null;
            // Unlike Knuth 6.4 Algorithm R, we must scan until
            // null because multiple entries could have been stale.
            while (tab[h] != null)
                h = nextIndex(h, len);
            tab[h] = e;
                }
            }
        }
```

当然，还有set时碰到被清理的节点时的替换情况，这里就不详细说了，我们主要就是对这个内部存储进行改造。

### 改进