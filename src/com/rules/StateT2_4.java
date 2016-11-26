package com.rules;

import com.XPath.PathParser.ASTPreds;
import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT2_4 extends StateT2 implements Cloneable{
    protected  State _q3;//检查 preds

    protected  StateT2_4(ASTPreds preds,State q3){
        super(preds);
        _q3=q3;
        _q3.setLevel(this.getLevel()+1);
        _predstack=new Stack();
    }

    public static StateT2 TranslateState(ASTPreds preds){//重新创建T2-4
        State q3=StateT3.TranslateStateT3(preds.getFirstStep().getPreds());
        return new StateT2_4(preds,q3);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
            // 等 q' 的结果
            addWTask(new WaitTask(layer, null,true));
            String name = ((Integer)this.hashCode()).toString().concat("T2-4.prActor");
            Actor actor;
            if(!actors.containsKey(name)){// 若 prActor还没有创建 ，predstack 一定为空
                actor=actorManager.createAndStartActor(TaskActor.class, name);
                _q3.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",
                        new Object[]{this._predstack,new ActorTask(layer, new Object[]{_q3,index,id}, false)});
                actorManager.send(dmessage, curactor, actor);
            }else{
                actor = actors.get(name);
                State currQ=(State) _q3.copy();
                currQ.setLevel(layer + 1);
                currQ.list = new ArrayList();
                ActorTask aatask = new ActorTask(layer,new Object[]{currQ,index,id},false);
                if(!actors.containsKey(name)){      //上一个的谓词已经检查成功弹栈了
                    System.out.println("，predstack 不为空，当前q3会add到curractor的缓存list中去");
                    //向 actor 发送数据块的 index + id
                    if(id == 1){
                        System.out.println(" 当前数据块处理结束，" + name + " 的Index：++index");
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{++index,0,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }else {
                        System.out.println("当前数据块还没结束，" + name + " 的Index：index");
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{index,++id,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }
                    return true;
                }else{
                    System.out.println("，predstack为空-即上一个q3已经检查成功弹栈了，当前q3直接压栈");
                    dmessage = new DefaultMessage("push",new ActorTask(layer, aatask, false));
                    actorManager.send(dmessage, curactor, actor);
                }
            }
            //向 actor 发送数据块的 index + id
            System.out.println(name + " 直接去cacheactor那里取数据块：++index/index");
            if(id == 1){
                dmessage = new DefaultMessage("modifyIndex", new Object[]{++index, 0});
                actorManager.send(dmessage, curactor, actor);
            }else {
                dmessage = new DefaultMessage("modifyIndex", new Object[]{index, ++id});
                actorManager.send(dmessage, curactor, actor);
            }
        }
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        if (atask.getId() == getLevel() - 1) {
            Stack ss = curactor.getMyStack();
            ActorTask task = ((ActorTask) ss.peek());//(id,T2-4,isInself)
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();
            //pop(T2-4)
            curactor.popFunction();
            //发消息（id,false,isInself）
            curactor.sendPredsResult(new ActorTask(idd, false, isInSelf));
            //当前栈不为空，栈顶进行endElementDo 操作（输出（T1-2或者T1-6）/弹栈（相同结束标签的waitState）等）
            if (!ss.isEmpty()) {
                State state=((State) (((ActorTask) ss.peek()).getObject()));
                // T1-2 、T1-6的结束标签
                if(state instanceof StateT1_2 || state instanceof StateT1_6){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }
        }
        return true;
    }

    /*
   * 收到谓词的返回结果：
   * 找到T2-4.list的中最后一个元素，设置
   * --若当前谓词满足了，还应该向上传递！！！
   * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(list.size()-1);//最后一个元素

        if(pred){    //true
            if(atask.isInSelf()){  //来自自己--设置的肯定是predR
                wt.setPredR(pred);
            }else{                 //来自T3.preds'--设置的肯定是pathR
                wt = (WaitTask)list.get(0);//第一个(id,null,null) // (id,true,null)
                wt.setPathR(pred);
            }

            {
                Stack ss = curractor.getMyStack();
                ActorTask task = ((ActorTask) ss.peek());//(id,T2-4,isInself)
                int idd = task.getId();
                boolean isInSelf = task.isInSelf();

                if(wt.isPredsSatisified()) { //(id,true,true)--上传
                    if(list.size() > 1) {  //原来是T3-1，现在只有T2-4 检查成功
                        curractor.sendPredsResult(new ActorTask(idd, true, true));        //给自己
                    } else {
                        curractor.popFunction(); //弹栈
                        curractor.sendPredsResult(new ActorTask(idd, true, isInSelf));  //给上级
                        if(!ss.isEmpty() && ((ActorTask) ss.peek()).getObject() instanceof StateT2_4){
                            curractor.processSameADPred();
                        }
                    }
                } else if(wt.isWaitT3ParallPreds()) { //(id,true,null)--(id,T2-4,isInself)换为（id,qw,isInself）
                    curractor.popFunction(); //弹栈
                    WaitState waitState = new WaitState();
                    waitState.setLevel(((State) task.getObject()).getLevel());
                    waitState.list.add(wt);
                    curractor.getMyStack().push(new ActorTask(idd, waitState, isInSelf));
                }
            }

        }else{     //false
            if(atask.isInSelf()){
                list.remove(list.size() - 1);
            }else
                list.clear();
        }
    }
}