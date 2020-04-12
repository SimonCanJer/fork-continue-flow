package com.async.fork.api;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

public class BUILDER {
    static Constructor<com.async.fork.api.ITaskPool> ctor =null;
    static com.async.fork.api.ITaskPool theInstance= null;
    static{
        try {
            Class<com.async.fork.api.ITaskPool> clazz = (Class<com.async.fork.api.ITaskPool>) Class.forName("com.async.fork.impl.TaskPool");
            ctor = clazz.getDeclaredConstructor(new Class[]{});
            ctor.setAccessible(true);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);

        }
    }

    static Callable<com.async.fork.api.ITaskPool> INSTANCE()
    {

        return new Callable<com.async.fork.api.ITaskPool>()
        {

            @Override
            public com.async.fork.api.ITaskPool call() throws Exception {
                if(theInstance==null)
                {
                    synchronized (BUILDER.class)
                    {
                        try {
                            theInstance= ctor.newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    }
                }
                return theInstance;
            };
        };

    }
}
