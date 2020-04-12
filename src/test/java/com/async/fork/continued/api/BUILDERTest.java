package com.async.fork.api;

import org.junit.Assert;
import org.junit.Test;

public class BUILDERTest {
    @Test
    public void INSTANCE() {
        com.async.fork.api.ITaskPool pool = null;
        Throwable error= null;
        try
        {
            pool= com.async.fork.api.BUILDER.INSTANCE().call();
        }
        catch(Throwable t)
        {
            error = t;
        }
        Assert.assertNull(error);
        Assert.assertNotNull("instance",pool);

    }

}