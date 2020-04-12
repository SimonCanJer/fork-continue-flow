package com.async.fork.api;

import java.io.Serializable;
import java.util.function.BiConsumer;

public interface IExecutionContext {
   <INPUT,OUTPUT> void childTask(INPUT input, com.async.fork.api.TaskNode<INPUT,OUTPUT> task, Serializable reminder, BiConsumer<OUTPUT,Serializable> callback, BiConsumer<Throwable,Serializable> errorHandler);
}
