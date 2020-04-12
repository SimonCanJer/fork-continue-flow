package com.async.fork.api;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public interface ITaskPool {
    Callable<ITaskPool> INSTANCE = com.async.fork.api.BUILDER.INSTANCE();
    <INPUT,OUTPUT> String createTask(com.async.fork.api.TaskNode<INPUT,OUTPUT> node, INPUT input, BiConsumer<OUTPUT,String> out, BiConsumer<Throwable,String> excpetionHandler);
}
