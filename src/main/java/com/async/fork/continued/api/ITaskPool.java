package com.async.fork.continued.api;

import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

public interface ITaskPool {
    Callable<ITaskPool> INSTANCE = BUILDER.INSTANCE();
    <INPUT,OUTPUT> String createTask(TaskNode<INPUT,OUTPUT> node, INPUT input, BiConsumer<OUTPUT,String> out, BiConsumer<Throwable,String> excpetionHandler);
}
