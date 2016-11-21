package com.rules;

import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by qin on 2015/10/10.
 */
public  abstract class State  implements Cloneable {
    protected int level;                                 //当前应该匹配的标签的层数
    public DefaultMessage dmessage;                       // 中间生成的消息
    public static DefaultActorManager actorManager = DefaultActorManager.getDefaultInstance();
    public static HashMap<String,Actor> actors = new HashMap();

    protected ArrayList list = new ArrayList();  //每一个 state 有一个 list，存放其 wt
    public  abstract boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException;
    public  abstract boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor);

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level=level;
    }

    public ArrayList getList() {
        return list;
    }

    public void addWTask(WaitTask wtask){
        if(!list.isEmpty() && list.get(list.size()-1) instanceof ArrayList){
            ((ArrayList<WaitTask>)list.get(list.size()-1)).add(wtask);
        }else{
            list.add(wtask);
        }
    }

    public void appendPathR(Object[] pathRs){
        int idd = (Integer)pathRs[0];
        int num = (Integer)pathRs[1];
        WaitTask waitTask = (WaitTask)pathRs[2];
        System.out.println(this + " 收到" + num + " 个需要append的pathR,pathR.id= "+idd);
        for(int i = list.size()-1;i >= 0;i--){
            ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(i);
            if(!llist.isEmpty() && llist.get(0).getId() == idd){
                for(int j = 0;j < num;j++){
                    llist.add(waitTask);
                }
                return;
            }
        }
    }

    public void setList(ArrayList list) {
        this.list = list;
    }

    public abstract Object copy() throws CloneNotSupportedException;

    public abstract void predMatchFunction(ActorTask atask,TaskActor curractor);

    public abstract void pathMatchFunction(ActorTask atask);


}
