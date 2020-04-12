package com.async.fork.continued.impl;

import com.async.fork.api.TaskNode;
import org.junit.Assert;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.junit.Assert.*;

public class ExecutionContextTest {

    ExecutionContext context = new ExecutionContext();

    @Test
    public void test() {
        String dir = System.getProperty("user.dir");
        System.out.println("user directory " + dir);
        init();
        childTask();
        test2Levels();
        testExceptionThrown();
        multilevelTask();

    }

    public void init() {
        context.init();
        Assert.assertEquals(context.executor.getCorePoolSize(), Runtime.getRuntime().availableProcessors());

    }

    static Map<Serializable, Throwable> mapRemErrors = new ConcurrentHashMap<>();

    static class Task extends TaskNode<Integer, Integer> {

        @Override
        public void execute(Integer in) {
            this.result(in + 1);
        }

        @Override
        protected <RESPONSE> void onResponse(RESPONSE res, Serializable s) {

        }

        @Override
        protected void onFailure(Throwable t, Serializable id) {
            mapRemErrors.put(id, t);

        }
    }

    int integerTaskRes = 0;
    String reminder = null;

    public void childTask() {
        mapRemErrors.clear();
        context.childTask(1, new Task(), "remainder", (res, serializable) -> {
            integerTaskRes = res;
            reminder = (String) serializable;

        }, (throwable, id) -> mapRemErrors.put(id, throwable));
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(mapRemErrors.size(), 0);
        Assert.assertEquals(integerTaskRes, 2);
        Assert.assertEquals(reminder, "remainder");

    }

    void test2Levels() {
        int[] refResult = new int[]{0};
        HierarhicTask sent;
        mapRemErrors.clear();
        context.childTask(new HierarhicArgument(), sent = new HierarhicTask(), 100, (res, serializable) -> refResult[0] = res, (throwable, id) -> mapRemErrors.put(id, throwable));

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(mapRemErrors.size(), 0);
        assertEquals(refResult[0], sent.returned);
        for (int i = 1; i <= 3; i++) {
            assertTrue(processedKeys.containsKey(i));
            assertTrue(processedKeys.containsKey(i));
        }
    }

    static class HierarhicArgument {
        int level = 1;
        int id = 0;
    }

    static ConcurrentHashMap<Integer, Integer> returnedKeys = new ConcurrentHashMap<>();
    static ConcurrentHashMap<Integer, Integer> processedKeys = new ConcurrentHashMap<>();

    static class HierarhicTask extends TaskNode<HierarhicArgument, Integer> {
        int returned = 0;


        @Override
        public void execute(HierarhicArgument in) {
            if (in.level == 1) {
                for (int i = 1; i <= 3; i++) {
                    in = new HierarhicArgument();
                    in.level = 2;
                    in.id = i;
                    fork(in, new HierarhicTask(), i);
                }
            } else {
                processedKeys.put(in.id, in.id);
                result(returned);
            }
        }

        @Override
        protected <RESPONSE> void onResponse(RESPONSE res, Serializable s) {
            returnedKeys.put((Integer) res, (Integer) s);
            if (++returned == 3) {
                result(returned);
            }

        }

        @Override
        protected void onFailure(Throwable t, Serializable id) {

        }
    }

    static class ExceptionThrowingTask extends TaskNode<Integer, Integer> {


        @Override
        public void execute(Integer in) {
            throw new NotImplementedException();
        }

        @Override
        protected <RESPONSE> void onResponse(RESPONSE res, Serializable s) {

        }

        @Override
        protected void onFailure(Throwable t, Serializable id) {

        }
    }

    public void testExceptionThrown() {
        Throwable[] thrown = new Throwable[1];
        String[] label = new String[1];
        boolean[] completeNotCalled = new boolean[]{true};
        context.childTask(1, new ExceptionThrowingTask(), "throw", (integer, serializable) -> completeNotCalled[0] = false, (throwable, marker) -> {
            label[0] = (String) marker;
            thrown[0] = throwable;

        });
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(completeNotCalled[0]);
        assertEquals(label[0], "throw");
        assertNotNull(thrown[0]);
        assertTrue(thrown[0] instanceof NotImplementedException);

    }

    static class HierarchicInput {
        int level = 1;
        String value = "1";
        HierarchicInput(String val)
        {
            value = val+"_";
        }
        HierarchicInput()
        {
            value = "level1_";
        }

        HierarchicInput nextLevel(int i) {
            HierarchicInput in = new HierarchicInput();
            in.level = level + 1;
            in.value = value + i;
            return in;

        }

    }

    static AtomicInteger totallyExecuted = new AtomicInteger(0);
    static int upperLevel = 0;
    static AtomicInteger openClosedBallance = new AtomicInteger(0);
    static AtomicInteger totalyExpectedNodes = new AtomicInteger(0);
   // static AtomicInteger totallyWorkedNodes = new AtomicInteger(0);

    static class TreeNodeTask extends TaskNode<HierarchicInput, Set<String>> {
        static final int MAX_LEVEL = 5;
        final Set<String> setResult = new HashSet<>();
        AtomicInteger ballance = new AtomicInteger(0);
        HierarchicInput input;

        @Override
        public void execute(HierarchicInput in) {
            synchronized (setResult) {
                System.out.println("MULTILEVEL TASK : level=" + in.level + " value " + in.value);
                setResult.add(in.value);
                input=in;
            }
            totallyExecuted.incrementAndGet();
            // ballance.incrementAndGet();
            upperLevel = in.level;
            openClosedBallance.incrementAndGet();
            if (in.level < MAX_LEVEL) {
                for (int i = 1; i <= MAX_LEVEL; i++) {
                    totalyExpectedNodes.incrementAndGet();
                    ballance.incrementAndGet();
                    HierarchicInput hi;
                    fork(hi = in.nextLevel(i), new TreeNodeTask(), "level=" + in.value + 1 + "call=" + i);
                    synchronized (expectedValues) {
                        expectedValues.add(hi.value);
                    }
                }
            } else {
                assertEquals("Upper Level must return 1", setResult.size(), 1);
                System.out.println("MULTILEVEL TASK : level=" + in.level + "tree is over, no fork, just return collected  " + setResult.size());
                result(setResult);
                openClosedBallance.decrementAndGet();
            }


        }

        @Override
        protected <RESPONSE> void onResponse(RESPONSE res, Serializable s) {
            synchronized (setResult) {
                System.out.println("MULTILEVEL TASK , level="+input.level+" response came, unresponded "+(ballance.get()-1));
                setResult.addAll((Set<String>) res);
            }
            if (ballance.decrementAndGet() == 0) {
                openClosedBallance.getAndDecrement();
                synchronized (setResult) {
                    System.out.println("MULTILEVEL TASK , level="+input.level+"all responded, returning "+setResult.size()+" collected values");

                    this.result(setResult);
                }
            }
            assertTrue("ballance not negative ", ballance.get() >= 0);

        }

        @Override
        protected void onFailure(Throwable t, Serializable id) {

        }
    }

    Set<String> result = null;
    static final Set<String> expectedValues = new HashSet<>();

    void multilevelTask() {
        System.out.println("****************TEST MULTILEVEL TASKS**************************");

        Throwable[] error = new Throwable[1];
        expectedValues.clear();
        Object o = new Object();
        totalyExpectedNodes.incrementAndGet();
        HierarchicInput in;
        in = new HierarchicInput();
        expectedValues.add(in.value);
        context.childTask(in, new TreeNodeTask(), "new task", new BiConsumer<Set<String>, Serializable>() {
            @Override
            public void accept(Set<String> strings, Serializable serializable) {
                result = strings;
                synchronized (o) {
                    o.notifyAll();
                }


            }
        }, (throwable, serializable) -> error[0] = throwable);
        try {
            synchronized (o) {
                o.wait(2000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertNull("no exceptions", error[0]);
        assertNotNull("result returned", result);
        assertEquals("worked levels =" + TreeNodeTask.MAX_LEVEL, upperLevel, TreeNodeTask.MAX_LEVEL);
        assertEquals("totally executed nodes == totally expected nodes", totallyExecuted.get(), totalyExpectedNodes.get());
        assertEquals("open close execution ballance =0", openClosedBallance.get(), 0);
        assertEquals("size of result == {number of executions}", result.size(), totallyExecuted.get());
        assertEquals("size of result == {number of executions}", result.size(), expectedValues.size());
    }
}