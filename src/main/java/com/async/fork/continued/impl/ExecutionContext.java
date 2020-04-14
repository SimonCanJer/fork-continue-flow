package com.async.fork.continued.impl;


import com.async.fork.continued.api.IExecutionContext;
import com.async.fork.continued.api.TaskNode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * This class fulfills asynchronous execution of tasks, parent-child connection,results handling, triggering
 * handlers are provided by tasks
 * @see TaskNode
 * The class hiddenly connectts parent and child tasks over dynamically denerated lambdas, which are really injected into
 * a child task, and then retrive and invokes original callbacks of parent task from a map (not directly) and sends a
 * wrapping runnable to common  executive queue
 * @see #q
 * The method childTask
 * @see #childTask(Object, TaskNode, Serializable, BiConsumer, BiConsumer, String)
 * do the manipulation with callbacks,puts original handlers for result and error to a special map,
 * puts the tasks into the execution queue
 * @see #q
 * @see #reactors
 * It is used to terminate exsisting childdren when task is terminated anyway (it is possible when a task generates
 * and error, and, even when terminated without waiting for all forkksa are completed.
 *
 * Important Note!
 * The task put in processing q only (!) once.
 * After it is retrieved and the execute(..) methods called,
 * @see TaskNode#execute(Object)
 * task will not be a subject for GC only(!) if it is forked during the execution. From this stage, the handlers
 * (lambdas)for  result or failure handlers and taskmap are the only anchors, which keeps the task instance alive
 * Their life time is till they are retrieved from the named map
 * After last forked task is completed and result have been processed, the TaskNode instance goes to GC.
 */

public class ExecutionContext  implements IExecutionContext {


    static class CallbackHolder<OUT>
    {
        final BiConsumer<OUT,Serializable> onResult;
        final BiConsumer<Throwable,Serializable> onFailed;
        List<String> generatedCallbacks=new CopyOnWriteArrayList<>();
        CallbackHolder(BiConsumer<OUT, Serializable> onResult, BiConsumer<Throwable, Serializable> onFailed) {
            this.onResult = onResult;
            this.onFailed = onFailed;
        }
    }
    ThreadPoolExecutor executor;
    BlockingQueue<Runnable> q = new LinkedTransferQueue<>();
    Map<String,CallbackHolder> reactors = new ConcurrentHashMap<>();
    void init()
    {
        int num = Runtime.getRuntime().availableProcessors();
        executor= new ThreadPoolExecutor(num,num,100000, TimeUnit.DAYS,q);
        executor.execute(new Runnable() {
            @Override
            public void run() {

            }
        });

    }
    ///Map<String, TaskNode> mapTasks = new ConcurrentHashMap<>();
   // Map<String, List<String>> mapTaskToChildren = new ConcurrentHashMap<>();

    void detachAllTheTree(CallbackHolder holder)
    {

        List<String> children=holder.generatedCallbacks;
        while(children.size()>0) {
            List<String> nextIteration = new ArrayList<>();
            for (String s : children) {
                CallbackHolder childHolder=reactors.remove(s);
                if(childHolder==null)
                    continue;
                nextIteration.addAll(childHolder.generatedCallbacks);
            }
            children = nextIteration;
        }

    }

    @Override
    public <INPUT, OUTPUT> void childTask(INPUT input, TaskNode<INPUT, OUTPUT> task, Serializable reminder, BiConsumer<OUTPUT, Serializable> callback, BiConsumer<Throwable,Serializable> errorHandler,String parent) {
        final String id= UUID.randomUUID().toString();
        Insert.setSystemID(task,id);
        if(parent!=null) {
            CallbackHolder parentHolder = reactors.get(parent);
            if(parentHolder!=null)
                parentHolder.generatedCallbacks.add(id);
        }
        BiConsumer<Throwable,String> errorCallback = new BiConsumer<Throwable,String>() {
            @Override
            public void accept(Throwable throwable,String id) {
                CallbackHolder<OUTPUT> holder = reactors.remove(id);
                detachAllTheTree(holder);
                if(holder!=null) {
                    q.add(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                holder.onFailed.accept(throwable, reminder);
                            }
                            catch(Throwable e)
                            {

                            }
                        }
                    });
                }
            }
        };
        BiConsumer<OUTPUT,String> onChildFinsihed = new BiConsumer<OUTPUT,String>() {

            @Override
            public void accept(OUTPUT output,String s) {
                CallbackHolder<OUTPUT> holder=reactors.remove(id);
                detachAllTheTree(holder);
                if(holder!=null)
                {
                    q.add(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                holder.onResult.accept(output, reminder);
                            }
                            catch(Throwable t)
                            {
                                try {
                                    holder.onFailed.accept(t, reminder);
                                }
                                catch (Throwable t1)
                                {

                                }
                            }
                        }
                    });
                }
            }
        };

        reactors.put(id,new CallbackHolder(callback,errorHandler));
        Insert.setCallback(task,onChildFinsihed);
        Insert.setContext(task,this);
        Insert.setFailureHandler(task, errorCallback);
        q.add(new Runnable() {
            @Override
            public void run() {
                try {
                    task.execute(input);
                }
                catch(Throwable error)
                {
                    errorHandler.accept(error,reminder);
                }
            }
        });


    }
}
