package com.async.fork.api;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class TaskNode<INPUT,OUTPUT> {

    INPUT input;
    OUTPUT output;
    com.async.fork.api.IExecutionContext context;
    private Consumer<OUTPUT> callback;
    private Consumer<Throwable> failure;
    public abstract void execute(INPUT in);
    protected abstract <RESPONSE> void onResponse(RESPONSE res, Serializable s);
    protected abstract void onFailure(Throwable t,Serializable id);
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
          });
      }

      protected void result(OUTPUT r)
      {
          callback.accept(r);

      }


      protected void failed(Throwable th){
        failure.accept(th);
      }

}
