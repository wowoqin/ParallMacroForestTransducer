package com.actormodel;

import com.ibm.actor.AbstractActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;
import com.rules.*;
import com.taskmodel.ActorTask;
import com.taskmodel.LinkList;
import com.taskmodel.Node;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2016/10/3.
 */
public class TaskActor extends AbstractActor {
    protected Stack myStack;    //每个actor 中应该有一个 stack ，也就是一个 stack 对应于一个 actor
    protected Actor resActor;  //上级 Actor
    protected LinkList linkList = CacheActor.linkList;

    public Stack getMyStack() {
        return myStack;
    }

    public void setMyStack(Stack myStack) {
        this.myStack = myStack;
    }

    public void setResActor(Actor resActor) {
        this.resActor = resActor;
    }

    public Actor getResActor() {
        return resActor;
    }

//        发送的消息--返回的结果（T/F/result）
//        接收的消息--输入用完--等待
//                 --数据块的引用--处理标签
//                 --返回的消息（T/F/result）
//    优先要处理谓词/path的返回结果是因为怕处理到结束标记的时候，还未处理返回结果（还得等待）--尴尬了就！！！
    @Override
    protected void loopBody(Message message) {
        sleep(1);
        String subject = message.getSubject();
        if("res&&push".equals(subject)){                // data是一个数组：data = {stack,task}--初始化
            State.actors.put(this.getName(),this);      //actors.put(this)
            Object[] datas = (Object[]) message.getData();
            this.setResActor(message.getSource());
            this.setMyStack((Stack) datas[0]);
            try {
                this.pushTaskDo((ActorTask)datas[1]);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }else if("push".equals(subject)){
            try {
                this.pushTaskDo((ActorTask)message.getData());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }else if("wait".equals(subject)){
            //输入用完--数据块没了，等 1s，然后继续发送修改index 的请求，看是否能成功吧
            Object[] data = (Object[])message.getData();

            try {
                Thread.sleep((Integer)data[1]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DefaultMessage message1 = new DefaultMessage("modifyIndex",(Integer)data[0]);
            this.getManager().send(message1, this, State.actors.get("cacheActor"));

        }else if("append".equals(subject)){
            this.appendPredR((Object[]) message.getData());
        }else{
            Stack  ss = this.getMyStack();
            State state = (State)((ActorTask)ss.peek()).getObject();  //栈顶 state
            // 收到actor的返回结果之后
            if("repred".equals(subject)){                      //T/F
                state.predMatchFunction((ActorTask)message.getData(),this);
            }else if("repath".equals(subject)){                //result
                state.pathMatchFunction((ActorTask)message.getData());
            }else if("nodeID".equals(subject)){               //数据块的引用(index+标签id)
                //从该Node中的第id个标签开始处理--交给栈顶 q
                Object[] data = (Object[])message.getData();

                int index = (Integer)data[0];    //根据index找到链表中的数据块
                int arrid = (Integer)data[1];
                Node node = linkList.getNode(index);   //一块只取一次--for循环之前得到

                for(int i = arrid;i<100;i++){          //从给定的id开始遍历处理标签
                    if(this.getMessageCount()>0){       //已经收到了返回结果
                        DefaultMessage message1 = new DefaultMessage("nodeID",new Object[]{index,i});
                        getManager().send(message1,this,this);
                        return; //中断此次处理--先处理返回的结果
                    }else{
                        ActorTask atask = node.getAtask()[i];//(layer,qName,true)
                        if(atask.isInSelf()){  //开始标签
                            try {
                                state.startElementDo(index,i,atask,this);
                            } catch (CloneNotSupportedException e) {
                                e.printStackTrace();
                            }
                        }else{    //结束标签
                            state.endElementDo(index,i,atask,this);
                        }
                        if(i==99){   //处理完当前数据块，需要指向下一块数据了
                            DefaultMessage message1 = new DefaultMessage("modifyIndex",index);
                            this.getManager().send(message1,this, State.actors.get("cacheActor"));
                        }
                    }
                }
            }
        }
    }

    //压栈操作
    public void pushTaskDo(ActorTask actorTask) throws CloneNotSupportedException {
        Stack curstack=this.getMyStack();
        State state=(State)(actorTask.getObject());     // 要压栈的 state

        if(state instanceof StateT3) {  //T3
            int id = actorTask.getId();
            boolean isInSelf = actorTask.isInSelf();
            int level = ((State) (actorTask.getObject())).getLevel();// T3 要匹配的层数

            State firstPred = ((StateT3) state).get_q2(); // q'''
            State remainPred = ((StateT3) state).get_q3();// q''
            firstPred.setLevel(level);
            remainPred.setLevel(level);
            //push(q''')
            curstack.push(new ActorTask(id, firstPred, isInSelf));
            //在 T3-1.q'''.list 中添加要等待的 wt
            firstPred.getList().add(new WaitTask(id, null, null));
            Stack stack = ((StateT3) state).get_predstack();

            String name = null;
            if (state instanceof StateT3_1)
                name = ((Integer) (stack).hashCode()).toString().concat("T3-1.prActor");
            else if (state instanceof StateT3_2)
                name = ((Integer) (stack).hashCode()).toString().concat("T3-2.prActor");
            else if (state instanceof StateT3_3)
                name = ((Integer) (stack).hashCode()).toString().concat("T3-3.prActor");
            else if (state instanceof StateT3_4)
                name = ((Integer) (stack).hashCode()).toString().concat("T3-4.prActor");
            //push(q'')-->继续调用此函数判断压栈
            if (stack.isEmpty()) {
                Actor actor = getManager().createAndStartActor(this.getClass(), name);
                DefaultMessage message = new DefaultMessage("res&&push", new Object[]{stack, new ActorTask(id, remainPred, false)});
                getManager().send(message, this, actor);
            } else {
                State currQ = (State) remainPred.copy();
                currQ.setLevel(level);
                DefaultMessage message = new DefaultMessage("push", new ActorTask(id, currQ, false));
                Actor actor = State.actors.get(name);
                getManager().send(message, this, actor);
            }
        }else{
            curstack.push(actorTask);
        }
    }

    public void popFunction(){
        Stack currStack = this.getMyStack();
        if(!currStack.isEmpty()){
            currStack.pop();
        }
    }

    public void sendPredsResult(ActorTask actorTask){   // 谓词检查成功，上传结果（id，true）给相应的 wt
        DefaultMessage message=new DefaultMessage("repred",actorTask);
        if(actorTask.isInSelf()){
            this.getManager().send(message, this, this);
        } else {
            TaskActor res = (TaskActor)this.getResActor();
            this.getManager().send(message, this, res);
        }
    }

    public boolean sendPathResult(ActorTask actorTask){    // path检查成功，上传结果（id，tag）给相应的 wt
        this.popFunction();//上传结果的时候肯定是遇到了上层结束标签，当前栈顶弹栈
        DefaultMessage message = new DefaultMessage("repath", actorTask);
        if(actorTask.isInSelf()){
            getManager().send(message, this, this);
        }else{
            TaskActor actor = (TaskActor)this.getResActor(); //上级actor
            State state = (State)((ActorTask)(actor.getMyStack()).peek()).getObject();//上级actor的栈顶 state
            if(state instanceof StateT1){
                getManager().send(message, this, actor);
            }else {
                //栈顶要是谓词(T1-6.preds)，则标签是传不过去的-->
                System.out.println("栈顶不是T1，无法完成path结果上传的操作！！！");
                return false;
            }
        }
        return true;
    }

    public void processSameADPred(){
        Stack currstack = this.getMyStack();
        if(!currstack.isEmpty()){  //传过去的结果只会对list的最后一个元素做检查--所以id就起到关键性的作用
            ActorTask task = (ActorTask)currstack.peek();
            int id = task.getId(); // 当前栈顶 taskmodel 的 id
            boolean isInSelf = task.isInSelf();
            this.popFunction();
            sendPredsResult(new ActorTask(id,true,isInSelf));
        }
        //栈为空
        this.getManager().detachActor(this);
    }

    public void processSameADPath(Object[] wt) {
        //找到上级actor当前栈顶的state的list---多个，最后一个list中add这些成功的 wt
        DefaultMessage message = new DefaultMessage("append",wt);
        getManager().send(message,this,resActor);
    }

    public void appendPredR(Object[] wt){  //追加AD轴path的检查结果--
        int num = (Integer)wt[0];
        WaitTask waitTask = (WaitTask)wt[1];
        while(!myStack.isEmpty()){
            State state = (State)((ActorTask)getMyStack().peek()).getObject();
            for(int i=0;i<num;i++){
                state.addWTask(waitTask);
            }
        }
    }

    public void output(WaitTask wt){
        wt.output();
    }
}
