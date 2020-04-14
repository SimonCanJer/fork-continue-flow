package com.async.fork.continued.impl;

import com.async.fork.continued.api.TaskNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.Serializable;

import static org.junit.Assert.*;

public class TaskPoolTest {

    TaskPool pool = new TaskPool();
    @Test
    public void  testAll()
    {
        testInitWithDummy();

    }
    Object response= null;
    String id=null;
    Throwable error=null;
    private void testInitWithDummy()
    {
        Object sync= new Object();
        String taskId=pool.createTask(new TaskNode<Object,Object>() {
            @Override
            public void execute(Object in) {
                result(in);

            }

            @Override
            protected void onFailure(Throwable t, Serializable id) {
                synchronized (sync)
                {
                    sync.notifyAll();
                }

            }

            @Override
            protected void onResponse(Object res, Serializable s) {

            }
        }, 1, (o, id)->{response=o;this.id=id;},(t, id)->{error=t;});
        synchronized (sync)
        {
            try {
                sync.wait(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Assert.assertNull(error);;
        assertNotNull(response);
        assertEquals(id,taskId);
    }

}