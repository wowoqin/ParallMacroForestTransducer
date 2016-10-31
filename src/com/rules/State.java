package com.rules;

import com.actormodel.TaskActor;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by qin on 2015/10/10.
 */
public  abstract class State  implements Cloneable {
    protected int level;                                 //当前应该匹配的标签的层数
    public DefaultMessage dmessage;                       // 中间生成的消息
    public static DefaultActorManager actorManager = DefaultActorManager.getDefaultInstance();
    public static Map<String,TaskActor> actors = new HashMap<String, TaskActor>();

    protected LinkedList list = new LinkedList();  //每一个 state 有一个 list，存放其 wt

    public  abstract void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException;
    public  abstract void endElementDo(int index,int id,ActorTask atask,TaskActor curactor);

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level=level;
    }

    public LinkedList getList() {
        return list;
    }

    public void addWTask(WaitTask wtask){
        if(!list.isEmpty() && list.get(list.size()-1) instanceof LinkedList){
            ((LinkedList<WaitTask>)list.get(list.size()-1)).add(wtask);
        }else{
            list.add(wtask);
        }
    }

    public abstract Object copy() throws CloneNotSupportedException;

    public abstract void predMatchFunction(ActorTask atask,TaskActor curractor);

    public abstract void pathMatchFunction(ActorTask atask);


}
