package com.async.fork.continued.api;

import java.lang.reflect.Constructor;
import java.util.concurrent.Callable;

public class BUILDER {
    static Constructor<ITaskPool> ctor =null;
    static ITaskPool theInstance= null;
    static{
        try {
            Class<ITaskPool> clazz = (Class<ITaskPool>) Class.forName("com.async.fork.impl.TaskPool");
            ctor = clazz.getDeclaredConstructor(new Class[]{});
            ctor.setAccessible(true);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);

        }
    }

    static Callable<ITaskPool> INSTANCE()
    {

        return new Callable<ITaskPool>()
        {

            @Override
            public ITaskPool call() throws Exception {
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
