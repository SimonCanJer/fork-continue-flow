package com.async.fork.continued.api;

import org.junit.Assert;
import org.junit.Test;

public class BUILDERTest {
    @Test
    public void INSTANCE() {
        ITaskPool pool = null;
        Throwable error= null;
        try
        {
            pool= BUILDER.INSTANCE().call();
        }
        catch(Throwable t)
        {
            error = t;
        }
        Assert.assertNull(error);
        Assert.assertNotNull("instance",pool);

    }

}