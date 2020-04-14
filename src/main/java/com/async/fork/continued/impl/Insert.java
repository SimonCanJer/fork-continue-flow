package com.async.fork.continued.impl;


import com.async.fork.continued.api.IExecutionContext;
import com.async.fork.continued.api.TaskNode;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class Insert {
    static private Field context;
    static private Field callback;
    static private Field errorHandler;
    static private Field systemID;

    static {
        try {
            context = TaskNode.class.getDeclaredField("context");
            context.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            callback = TaskNode.class.getDeclaredField("callback");
            callback.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        try {
            errorHandler = TaskNode.class.getDeclaredField("failure");
            errorHandler.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        try {
            systemID = TaskNode.class.getDeclaredField("systemID");
            systemID.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    static void setContext(TaskNode task, IExecutionContext ctx)
    {
        try {
            context.set(task,ctx);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    static void setCallback(TaskNode task, BiConsumer c)
    {
        try {
            callback.set(task,c);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    static void setFailureHandler(TaskNode task, BiConsumer c)
    {
        try {
            errorHandler.set(task,c);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static void setSystemID(TaskNode task,String id)
    {
        try {
            systemID.set(task,id);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
