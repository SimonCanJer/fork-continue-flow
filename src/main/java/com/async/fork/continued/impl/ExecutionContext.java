package com.async.fork.continued.impl;

import com.async.fork.api.IExecutionContext;
import com.async.fork.api.TaskNode;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ExecutionContext  implements IExecutionContext {

    ThreadPoolExecutor executor;
    BlockingQueue<Runnable> q = new LinkedTransferQueue<>();
    Map<String,BiConsumer<?,Serializable>> reactors = new ConcurrentHashMap<>();
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
    @Override
    public <INPUT, OUTPUT> void childTask(INPUT input, TaskNode<INPUT, OUTPUT> task, Serializable reminder, BiConsumer<OUTPUT, Serializable> callback,BiConsumer<Throwable,Serializable> errorHandler) {
        final String id= UUID.randomUUID().toString();
        reactors.put(id,callback);
        Consumer<OUTPUT> inject = new Consumer<OUTPUT>() {
            Consumer<Throwable> errorCallback = new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    errorHandler.accept(throwable,reminder);
                }
            };
            @Override
            public void accept(OUTPUT output) {
               BiConsumer<OUTPUT,Serializable> cb= (BiConsumer<OUTPUT, Serializable>) reactors.remove(id);
               if(cb!=null)
               {
                   try {
                       cb.accept(output, reminder);
                   }
                   catch(Throwable t)
                   {
                       errorHandler.accept(t,reminder);

                   }
               }
            }
        };

        Insert.setCallback(task,inject);
        Insert.setContext(task,this);
        Insert.setFailureHandler(task, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                errorHandler.accept(t,reminder);
            }
        });
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
