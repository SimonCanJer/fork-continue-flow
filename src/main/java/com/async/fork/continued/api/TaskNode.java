package com.async.fork.continued.api;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The TaskNode is the base class of any task of fork-continue pool, which can be accepted by the pool
 * The task has only one method, which will be called upon execution mechanism of the pool
 * @see com.async.fork.continued.impl.ExecutionContext
 * will take the task from its queue and will be called only at this time(only once)
 * If no fork is done, the task becomes to be the GC subject. In order
 * to returun a value to teh task resul listener, the task must call the
 * @see #result(Object)  method
 * Task can prolong its life and execute a workflow by mean of forks.
 * In this case task we be kept in memory by exposed listeners untill last of forks replied with result.
 * The it will be yet once a subject of GC.
 * Fork is done by means of calling the method
 * @see #fork(Object, TaskNode, Serializable)
 * and result can be handled by
 * @see #onResponse(Object, Serializable)  or
 * @see #onFailure(Throwable, Serializable)  if exception occured.
 * Task shoudl  call special method, to inform listener about error
 * @see #failed(Throwable)
 * Upon either result() or failed method have been called, task removed from listening together with all its children
 * @param <INPUT> input type
 * @param <OUTPUT> output type;
 */
public abstract class TaskNode<INPUT,OUTPUT> {

    INPUT input;
    OUTPUT output;
    IExecutionContext context;
    private BiConsumer<OUTPUT,String> callback;
    private BiConsumer<Throwable,String> failure;

    /**
     * declaration of main method of the task  which will be called after the task extracted from message queue
     * @param in parameter
     */
    public abstract void execute(INPUT in);

    /**
     * declaration of action to handle  result of a fork task
     * @param res   the result value
     * @param marker the same value of marker (reminder) which was used in fork
     * @see #fork(Object, TaskNode, Serializable)
     * @param <RESPONSE>
     */
    protected abstract <RESPONSE> void onResponse(RESPONSE res, Serializable marker);

    /**
     * declaration of action, which will be  called upon a fork task have returned an error
     * @param t        value of the error returned
     * @param id       the same value of marker (reminder) which was used in fork
     *  @see #fork(Object, TaskNode, Serializable)
     */
    protected abstract void onFailure(Throwable t,Serializable id);
        String    systemID;

    /**
     * call this method to create a new fork, which will be responded
     * @param req      request argument
     * @param task     a new task
     * @param reminder any value, which helps to identify a result (or error) has been returned.
     * @param <REQUEST>
     * @param <RESPONSE>
     */
    protected <REQUEST,RESPONSE> void fork(REQUEST req,TaskNode<REQUEST,RESPONSE> task,Serializable reminder)
    {
          context.childTask(req, task, reminder, new BiConsumer<RESPONSE, Serializable>() {
              @Override
              public void accept(RESPONSE response, Serializable serializable) {
                  onResponse(response, serializable);
              }
          }, new BiConsumer<Throwable, Serializable>() {
              @Override
              public void accept(Throwable throwable, Serializable serializable) {

              }
          },systemID);
      }

    /**
     * call tje method to return result
     * @param r - result value
     */
      protected void result(OUTPUT r)
      {
          callback.accept(r,systemID);

      }

    /**
     * call the method to return error
     * @param th error value
     */
      protected void failed(Throwable th){
        failure.accept(th,systemID);
      }

}
