package com.async.fork.continued.api;

import java.io.Serializable;
import java.util.function.BiConsumer;

public interface IExecutionContext {
   <INPUT,OUTPUT> void childTask(INPUT input, TaskNode<INPUT,OUTPUT> task, Serializable reminder, BiConsumer<OUTPUT,Serializable> callback, BiConsumer<Throwable,Serializable> errorHandler,String parent);
}
