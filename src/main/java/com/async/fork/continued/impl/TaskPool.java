package com.async.fork.continued.impl;

import com.async.fork.continued.api.ITaskPool;
import com.async.fork.continued.api.TaskNode;

import java.io.Serializable;
import java.util.UUID;
import java.util.function.BiConsumer;

public class TaskPool implements ITaskPool {
    com.async.fork.continued.impl.ExecutionContext contex= new com.async.fork.continued.impl.ExecutionContext();
    volatile boolean init=true;

    @Override
    public <INPUT, OUTPUT> String createTask(TaskNode<INPUT, OUTPUT> task, INPUT input, BiConsumer<OUTPUT,String> out, BiConsumer<Throwable,String> errorHandler) {
        if(init){
            synchronized (this)
            {
                if(init)
                {
                    init = false;
                }
                contex.init();
            }
        }
        String resId= UUID.randomUUID().toString();
        contex.childTask(input, task, null, new BiConsumer<OUTPUT, Serializable>() {
            @Override
            public void accept(OUTPUT output, Serializable serializable) {
                out.accept(output, resId);
            }
        }, new BiConsumer<Throwable, Serializable>() {
            @Override
            public void accept(Throwable throwable, Serializable serializable) {
                errorHandler.accept(throwable,resId);
            }
        },resId);
        return resId;
    }
}
