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

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Created by qin on 2016/10/3.
 */
public class TaskActor extends AbstractActor {
    protected Stack myStack;    //每个actor 中应该有一个 stack ，也就是一个 stack 对应于一个 actor
    protected Actor resActor;   //上级 Actor
    protected LinkList linkList = CacheActor.linkList;
    protected int currIndex = 0; //当前数据块索引
    protected int currId = 0;  //当前索引内的标签下标
    protected int waitIndex = 0;   //新数据块索引
    protected int waitId = 0;      //新索引内的新标签下标
    protected ArrayList<Object[]> mylist = new ArrayList<>();

    public ArrayList<Object[]> getMylist() {
        return mylist;
    }

    public int getWaitId() {
        return waitId;
    }

    public int getWaitIndex() {
        return waitIndex;
    }

    public void setWaitIndex(int waitIndex) {
        this.waitIndex = waitIndex;
    }

    public void setWaitId(int waitId) {
        this.waitId = waitId;
    }

    public int getCurrIndex() {
        return currIndex;
    }

    public int getCurrId() {
        return currId;
    }

    public void setCurrIndex(int currIndex) {
        this.currIndex = currIndex;
    }

    public void setCurrId(int currId) {
        this.currId = currId;
    }

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


    @Override
    public void activate() {
        super.activate();
    }
    @Override
    public void deactivate() {
        super.deactivate();
    }
    @Override
    public boolean willReceive(String subject) {
        return super.willReceive(subject);
    }
    @Override
    public int getMaxMessageCount() {
        return 1000000000;
    }

    @Override
    protected void runBody() {}

    @Override
    public void addMessage(DefaultMessage message) {
        if(message != null) {
            List var2 = this.messages;
            synchronized(this.messages) {
                if(this.messages.size() >= this.getMaxMessageCount()) {
//                    System.out.println(this.messages.size());
//                    throw new IllegalStateException("too many messages, cannot add");
                }

                this.messages.add(message);
            }
        }

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
            System.out.println(this.getName() + " 的初始操作--res&&push");
            State.actors.put(this.getName(), this);
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
        }else if("needModifyIndex".equals(subject)){
            //先看队列为空么，为空--先取到index和id，和currentindex和currentid比较，设置waitIndex和waitId，
            //              不为空--直接入队
            Object[] data = (Object[]) message.getData();
            int index = (Integer) data[0];
            int id = (Integer) data[1];
            System.out.print(this.getName() + ".needModifyIndex操作，");
            if(myStack.isEmpty()){
                System.out.println("needModifyIndex中当前栈为空(pred检查成功直接已经弹栈的)，直接压栈&直接去请求数据块");
                try {
                    this.pushTaskDo((ActorTask)data[2]);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }

                DefaultMessage message1 = new DefaultMessage("modifyIndex",new Object[]{index,id});
                this.getManager().send(message1, this, State.actors.get("cacheActor"));

                if(!this.mylist.isEmpty()){
                    Object[] tuple = mylist.get(0);
                    setWaitIndex((Integer)tuple[0]);
                    setWaitId((Integer)tuple[1]);
                }
            }else{
                if(!this.mylist.isEmpty()){
                    System.out.println("但当前栈不为空&mylist不为空：直接添加");
//                    Object[] first = mylist.get(0);
                    if(index < waitIndex || (index == waitIndex && id < waitId)){
                        mylist.add(0,data);   //确保list中第一位是最小的index--/b/c的这种情况
                        setWaitIndex(index);
                        setWaitId(id);
                    } else
                        mylist.add(data);
                }else{
                    //第一次添加的时候不可能当前actor就处理到了要修改的标签处，
                    // 肯定currIndex < waitIndex ||(currIndex < waitIndex && currId < waitId)
                    System.out.println(this.getName()+" 第一次在mylist中添加");
                    mylist.add(data);
                    setWaitIndex(index);
                    setWaitId(id);
                }
            }
        }else if("modifyIndex".equals(subject)){
            this.getManager().send(message, this, State.actors.get("cacheActor"));
        } else if("wait".equals(subject)){
            //输入用完--数据块没了，等 10ms，然后继续发送修改index 的请求，看是否能成功吧
            System.out.print(this.getName() + " 接到消息：cacheactor 中输入用完--需要等待，");
            Object[] data = (Object[])message.getData();

            if(this.getMessageCount() > 0){
                System.out.print("但 messagecount > 0,先去处理其他消息吧");
            }else{
                System.out.println("sleep一会继续 modifyIndex");
                try {
                    Thread.sleep((Integer)data[1]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            DefaultMessage message1 = new DefaultMessage("modifyIndex",new Object[]{data[0],0});
            this.getManager().send(message1, this, State.actors.get("cacheActor"));
        }else {
            Stack  ss = this.getMyStack();
            if(!ss.isEmpty()){
                State state = (State)((ActorTask)ss.peek()).getObject();  //栈顶 state
                // 收到actor的返回结果之后
                if("append".equals(subject)){
                    state.appendPathR((Object[]) message.getData());
                }else if("repred".equals(subject)){                      //T/F
                    System.out.print(this.getName() + " 收到predR，栈顶 ");
                    if(state instanceof StateT2_4){
                        if(message.getSource().getName().contains("T3"))
                            state.predMatchFunction((ActorTask)message.getData(),this);
                        else {  //(id,true/false,true)--自身检查成功
                            ActorTask task = (ActorTask)message.getData();
                            state.predMatchFunction(new ActorTask(task.getId(),task.getObject(),true),this);
                        }
                    }else
                        state.predMatchFunction((ActorTask)message.getData(),this);
                }else if("repath".equals(subject)){                //result
                    System.out.print(this.getName() + " 收到pathR，栈顶 ");
                    state.pathMatchFunction((ActorTask)message.getData());
                }else if("nodeID".equals(subject)){               //数据块的引用(index+标签id)
                    //从该Node中的第id个标签开始处理--交给栈顶 q
                    Object[] data = (Object[])message.getData();

                    int index = (Integer)data[0];    //根据index找到链表中的数据块
                    int arrid = (Integer)data[1];
                    setCurrIndex(index);

                    Node node = linkList.getNode(index);   //一块只取一次--for循环之前得到
                    System.out.println(this.getName() + " 收到数据块的 nodeID = " + index +"--for循环处理标签");


                    if(getCurrIndex() == this.waitIndex && !mylist.isEmpty()){
                        System.out.println(this.getName() + " 的缓存队列中的元素个数是：" + mylist.size());
                        for(int i = arrid;i < 2;i++){             //从给定的id开始遍历处理标签
                            if(!getMyStack().isEmpty()){
                                this.currId = i;
                                if(this.currId == this.waitId){
                                    //先push，再取栈顶
                                    Object[] tuple = this.mylist.get(0);
                                    try {
                                        this.pushTaskDo((ActorTask)tuple[2]);
                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                    this.mylist.remove(0);
                                    if(!this.mylist.isEmpty()){
                                        tuple = this.mylist.get(0);
                                        setWaitIndex((Integer)tuple[0]);
                                        setWaitId((Integer)tuple[1]);
                                    }
                                }

                                state = (State)((ActorTask)ss.peek()).getObject();  //每次更新栈顶 state
                                ActorTask atask = node.getAtask()[i];   //(layer,qName,true)
                                boolean flg = false;

                                if(atask.isInSelf()){  //开始标签
                                    System.out.println(this.getName() + " 的for循环中处理开始标签：" + atask.getObject().toString());
                                    try {
                                        flg = state.startElementDo(index,i,atask,this);
                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                }else{    //结束标签
                                    System.out.println(this.getName() + " 的for循环中处理结束标签：" + atask.getObject().toString());
                                    flg  = state.endElementDo(index,i,atask,this);
                                }

                                if(!flg)
                                    return;
                                else if(i==1 && !this.getMyStack().isEmpty()){   //处理完当前数据块，需要指向下一块数据了
//                                    System.out.println(this.getName() + " 对当前数据块for循环处理结束--要求去modifyIndex");
                                    while(State.actors.get("cacheActor").getMessageCount() == 100){
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    DefaultMessage message1 = new DefaultMessage("modifyIndex", new Object[]{++index,0});
                                    this.getManager().send(message1, this, State.actors.get("cacheActor"));
                                }
                            }
                        }
                    }else{   //直接处理
                        for(int i = arrid;i < 2;i++){             //从给定的id开始遍历处理标签
                            if(!this.getMyStack().isEmpty()){
                                state = (State)((ActorTask)ss.peek()).getObject();  //每次更新栈顶 state
                                ActorTask atask = node.getAtask()[i];   //(layer,qName,true)
                                boolean flg = false;

                                if(atask.isInSelf()){  //开始标签
                                    System.out.println(this.getName() + " 的for循环中处理开始标签：" + atask.getObject().toString());
                                    try {
                                        flg = state.startElementDo(index,i,atask,this);
                                    } catch (CloneNotSupportedException e) {
                                        e.printStackTrace();
                                    }
                                }else{    //结束标签
                                    System.out.println(this.getName() + " 的for循环中处理结束标签：" + atask.getObject().toString());
                                    flg  = state.endElementDo(index,i,atask,this);
                                }

                                if(!flg)
                                    return;
                                else if(i==1 && !this.getMyStack().isEmpty()){   //处理完当前数据块，需要指向下一块数据了
                                    System.out.println(this.getName() + " 对当前数据块for循环处理结束--要求去modifyIndex");

                                    while(State.actors.get("cacheActor").getMessageCount() == 100){
                                        try {
                                            Thread.sleep(1);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    DefaultMessage message1 = new DefaultMessage("modifyIndex", new Object[]{++index,0});
                                    this.getManager().send(message1, this, State.actors.get("cacheActor"));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //压栈操作
    public void pushTaskDo(ActorTask actorTask) throws CloneNotSupportedException {
        int id = actorTask.getId();
        boolean isInSelf = actorTask.isInSelf();
        Object[] obj = ((Object[]) actorTask.getObject());
        State state = (State)obj[0];
        System.out.println(this.getName() + " 进行压栈操作," + state +" 入栈");

        if(state instanceof StateT3) {  //T3
            //actorTask = (id,[q3,index,arrid],isInself)
            int index = (Integer)obj[1];
            int arrid = (Integer)obj[2];
            int level = state.getLevel();// T3 要匹配的层数
            State firstPred = ((StateT3) state).get_q2(); // q'''
            State remainPred = ((StateT3) state).get_q3();// q''
            firstPred.setLevel(level);
            remainPred.setLevel(level);
            //push(q''')
            this.myStack.push(new ActorTask(id, firstPred, isInSelf));
            //在 T3-1.q'''.list 中添加要等待的 wt
            firstPred.getList().add(new WaitTask(id, null, null));
            Stack prstack = ((StateT3) state).get_predstack();
            String name = null;
            Actor actor;
            ActorTask aatask;

            if (state instanceof StateT3_1)
                name = ((Integer) this.hashCode()).toString().concat("T3-1.prActor");
            else if (state instanceof StateT3_2)
                name = ((Integer) this.hashCode()).toString().concat("T3-2.prActor");
            else if (state instanceof StateT3_3)
                name = ((Integer) this.hashCode()).toString().concat("T3-3.prActor");
            else if (state instanceof StateT3_4)
                name = ((Integer) this.hashCode()).toString().concat("T3-4.prActor");
            //push(q'')-->继续调用此函数判断压栈
            if (!State.actors.containsKey(name)) {
                actor = getManager().createAndStartActor(this.getClass(), name);
                DefaultMessage message = new DefaultMessage("res&&push", new Object[]{prstack, new ActorTask(id, new Object[]{remainPred,index,arrid}, false)});
                getManager().send(message, this, actor);
            } else {
                actor = State.actors.get(name);
                State currQ = (State) remainPred.copy();
                currQ.setLevel(level);
                currQ.setList(new ArrayList());
                if(!prstack.isEmpty()){      //上一个的谓词已经检查成功弹栈了
                    System.out.println("，predstack 不为空，当前q3会add到curractor的缓存list中去");
                    aatask = new ActorTask(level,new Object[]{currQ,index,arrid},false);
                    //向 actor 发送数据块的 index + id
                    if(id == 1){
                        System.out.println(" 当前数据块处理结束，" + name + " 的Index：++index");
                        DefaultMessage message = new DefaultMessage("needModifyIndex", new Object[]{++index,0,aatask});
                        getManager().send(message, this, actor);
                    }else {
                        System.out.println("当前数据块还没结束，" + name + " 的Index：index");
                        DefaultMessage message = new DefaultMessage("needModifyIndex", new Object[]{index,++arrid,aatask});
                        getManager().send(message, this, actor);
                    }
                    return;
                }else{
                    System.out.println("，predstack为空-即上一个q3已经检查成功弹栈了，当前q3直接压栈");
                    DefaultMessage message = new DefaultMessage("push", new ActorTask(id, new Object[]{currQ,index,arrid}, false));
                    getManager().send(message, this, actor);
                }
            }

            //向 actor 发送数据块的 index + id
            System.out.println(name + " 直接去cacheactor那里取数据块：++index/index");
            if(arrid == 1){
                DefaultMessage message = new DefaultMessage("modifyIndex", new Object[]{++index ,0});
                getManager().send(message, this, actor);
            }else {
                DefaultMessage message = new DefaultMessage("modifyIndex", new Object[]{index, ++arrid});
                getManager().send(message, this, actor);
            }
        }else{
            //(id,[q],isInself)
            myStack.push(new ActorTask(id,state,isInSelf));
        }
    }

    public void popFunction(){
        System.out.println(this.getName() + " 弹栈操作");
        Stack currStack = this.getMyStack();
        if(!currStack.isEmpty()){
            currStack.pop();
        }
    }

    public void sendPredsResult(ActorTask actorTask){   // 谓词检查成功，上传结果（id，true）给相应的 wt
        DefaultMessage message=new DefaultMessage("repred",actorTask);
        if(actorTask.isInSelf()){
            System.out.println(this.getName() + " 上传谓词结果给自己");
            this.getManager().send(message, this, this);
        } else {
            System.out.println(this.getName() + " 上传谓词结果给上级");
            this.getManager().send(message, this, this.getResActor());
        }
    }

    public boolean sendPathResult(ActorTask actorTask){    // path检查成功，上传结果（id，tag）给相应的 wt
        this.popFunction();//上传结果的时候肯定是遇到了上层结束标签，当前栈顶弹栈
        DefaultMessage message = new DefaultMessage("repath", actorTask);
        if(actorTask.isInSelf()){
            System.out.println(this.getName() + " 上传 path 结果给自己");
            getManager().send(message, this, this);
        }else{
            System.out.println(this.getName() + " 上传path结果给上级");
            TaskActor actor = (TaskActor)this.getResActor(); //上级actor
            State state = (State)((ActorTask)(actor.getMyStack()).peek()).getObject();//上级actor的栈顶 state
            if(state instanceof StateT1){
                getManager().send(message, this, actor);
            }else {
                //栈顶要是谓词(T1-6.preds)，则标签是传不过去的-->
                System.out.println(" 栈顶不是T1，无法完成path结果上传的操作！！！");
                return false;
            }
        }
        return true;
    }

    public void processSameADPred(){
        System.out.println(this.getName() + " processSameADPred 操作");
        Stack currstack = this.getMyStack();
        while(!currstack.isEmpty()){    //传过去的结果只会对list的最后一个元素做检查--所以id就起到关键性的作用
            ActorTask task = (ActorTask)currstack.peek();
            int id = task.getId();   // 当前栈顶 taskmodel 的 id
            boolean isInSelf = task.isInSelf();
            this.popFunction();
            this.sendPredsResult(new ActorTask(id, true, isInSelf));
        }
    }

    public void processSameADPath(Object[] wt) {
        //找到上级actor当前栈顶的state的list---多个，最后一个list中add这些成功的 wt
        System.out.println(this.getName() +" processSameADPath 操作");
        DefaultMessage message = new DefaultMessage("append",wt);
        getManager().send(message,this,this.getResActor());
    }

    public void processEmStackANDNoEmMylist(){
        System.out.println("当前栈为空&&mylist不为空，mylist.get(0)压栈&去请求数据块");
        Object[] data = mylist.get(0);
        try {
            this.pushTaskDo((ActorTask)data[2]);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        DefaultMessage message1 = new DefaultMessage("modifyIndex",new Object[]{data[0],data[1]});
        this.getManager().send(message1, this, State.actors.get("cacheActor"));

        if(!this.mylist.isEmpty()){
            Object[] tuple = mylist.get(0);
            setWaitIndex((Integer)tuple[0]);
            setWaitId((Integer)tuple[1]);
        }
    }

    public void output(WaitTask wt){
        wt.output();
    }
}
