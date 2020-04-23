# fork-continue-flow




The reason of the projects is an alternative way of  asynchronous of  chain of forked tasks. The Fork/Join framework is a well known tool and does not needs any introducing remarks.
The Fork/Join framework uses the joint concept, it can fork a task execution and joints the forked task. Under the hood it joints  a fiber ( a used thread in a domestic JVM). This model does not use separated OS threads per tasks (user thread instead of it is used), OS thread really does not wait for a task execution (just resumes fiber's stack) , but still allocates stacks for fibers and uses wait model.
The recent exercise project demonstrates an alternative idea: a  fork – continue  concept, when no one task, which has created a fork, is pending for the  fork execution, but continues worklfow by handling callbacks (or events)  from generated   fork tasks (either result, or failure) in absolutely asynchronous mode, same as execution of initial and forked tasks. The stack of either direct (execute) or callback is just one method with no  recursions. Any thread from thread pool executor of the ExecutionService executes  a queued Runnable, which  envelops either a task with parameters, or  a callback, which calls the parent task.
Therefore, we have the following model of work

                  Tasks

  Any Task object extends the abstrcat class TaskNode , which have the abstract interface method execute(...) and the protected abstract methods onFailure(Throwable,Serializable), onResult(RESULT,Serializable). Besides, the TaskNode class has also the method fork(TaskNode, Serializable,)  which gets a new tasks instance to be executed  and any reminder object.   The reminder will be returned back by system, when triggering the onResult/onFailure in order to simplify identification of an event.    Work of a NodeTask is completed by means of calling either of the methods : doResult(RESULT)-when completed successfully, , or failure(Throwable)-when error occurred. If a not trapped exception occurred, then task will be terminated, its children removed and a parent will get notification (onFailure) about the case, or if it is an initial task, then related callback will be called directly.
  A NodeTask contains and uses also an interface IExecutionContext, which is just an entry point to executive poll.  Inside a call of the fork(..) method, a BiConsumers (callbacks) a created to handle failure or completion of entire forked task. The callbacks just call onFailure and onResult methods, which are overriden by a real task class.
   A NodeTask derived object has a life cycle, which  is initiated by call of the execute(..) method, inside of which  either work must be completed by means of calling doResult( or failure()), or a fork should be called with a newly created task. The life cycle of a task continued, if the fork(...) called in the  the onResult(..) or onFailure methods, which are  called by the system over callbacks which are provided to container inside the fork().If  fork has not been called, then  a task becomes to be a subject of GC after  the execute(...) method called. If fork called, then task guaranty will be kept till last of callbacks called. 



Task pool and execution context.

The main work of the model is executed by the ExecutionContext  class.
Execution context makes all the work including:
1. task queuing in a polled queue (EXECUTIVE QUEUE , class : ConcurrentLinkedQueue)
2. callbacks registration for a task (in CALLBACK TABLE class: ConcurrentHashMap)
3. links fork and parent task for asynchronous callback execution upon fork tsk completed
4. Queueing and asynchronous call of  callbacks (in the EXECUTIVE QUEUE).
  
     Thus, the model and  workflow of ExecutionContext is:
Creates EXECUTIVE QUEUE of Runnables, which is polled by a thread pool executor. 
- Creates CALLBACK TABLE, where  forked task callbacks and reminders are registered 
- Creates and starts  thread pool executor polling the EXCUTIVE QUEUE. \
- Any polled Runnable   either callsexecute(...) of a TaskNode  with parameter, or calls callback with results (or with an error info.)
When a fork  task arrived,  the ExecutionContext injects  its invocation interface into the task, which will be used by its fork method, also, it registers incoming original callbacks from the parent task for result and failure notification, by putting it into the CALLBACK TABLE 
- Before the fork task will be added into the EXECUTIVE QUEUE, the ExecutionContext creates and injects the new callbacks (BiConsumers) into the fork task object: the newly created callbacks  will find original callbacks holder in the CALLBACK TABLE, create a Runnable, which envelops invoke of  the relevant original callback and puts the Runnable into the executive queue. 
- Initially (without any forking) inside the task, ExecutionContext just calls execute(...) of the NodeTask. Task itself is kept in no place and becomes to be a subject of GC. 
- If, it calls fork inside the execute(..) method, then the returned callbacks, which are registered in the CALLBACK TABLE (they generated under the hood and contain “this” or a parent task) are the only anchors, which is guaranty for the task to be outside of GC  collection. 
- If any task calls either  doResult of failure(..), then the the callback, which is generated by the ExecutionContext, all the tree of its child tasks and callbacks will the removed from the EXECUTIVE QUEUE and CALLBACK TABLE. 
- Even if  callback called, then it does not lead to sequential calls of callbacks in the same tread- ExecutionContex generated a new Runnable and subsequent call down of callbacks will be done asynchronously and among other runs
