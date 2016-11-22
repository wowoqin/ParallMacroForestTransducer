package com.rules;

import com.actormodel.TaskActor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2016/4/18.
 */
public class WaitState extends State {

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {
        return true;
    }
    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        // 自己能遇到上层结束标签，谓词检查失败，弹栈 && remove 等待当前栈顶 qw 结果的 wt
        int layer = atask.getId();

        if (layer == getLevel() - 1) {
            //遇到上层结束标签但是preds'还没返回结果，等
            if(!list.isEmpty()){
                WaitTask wt = (WaitTask)list.get(0);
                if(!wt.hasReturned()){
                    System.out.println("qw.preds'还未返回结果");
                    do{
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while(curactor.getMessageCount() == 0);

                    System.out.println("qw.pred'返回结果了--先去处理 predR");
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage,curactor,curactor);
                    return false; //中断此次处理--先处理返回的结果
                }
            }

            Stack ss = curactor.getMyStack();
            ActorTask task=((ActorTask) ss.peek());//(id,qw,isInself)
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();
            //pop(qw)
            curactor.popFunction();
            //发消息（id,false,isInself）
            curactor.sendPredsResult(new ActorTask(idd, false, isInSelf));
            //当前栈不为空，栈顶进行endElementDo 操作（输出（T1-2或者T1-6）/弹栈（相同结束标签的waitState）等）
            if (!ss.isEmpty()) {
                State state = ((State) (((ActorTask) ss.peek()).getObject()));
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

    @Override
    public Object copy() throws CloneNotSupportedException {
        return null;
    }
    @Override
    public void pathMatchFunction(ActorTask atask) {

    }
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        //(id,true,null)--所以返回结果都是来自T3.preds'--设置的肯定是pathR
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(0);   //只有一个元素
        System.out.print("qw 处理 predR，");
        if(pred){    //true
            if(atask.isInSelf()){  //来自自己--T2-1检查成功
                wt.setPredR(pred);
            }else{   //来自T3.preds'
                wt.setPathR(pred);
            }

            //设置完检查当前wt的表现形式
            {
                Stack ss = curractor.getMyStack();
                ActorTask task = ((ActorTask) curractor.getMyStack().peek());//(id,qw,isInself)
                int idd = task.getId();
                boolean isInSelf = task.isInSelf();

                if(wt.isPredsSatisified()) {   //(id,true,true)--T3-1检查成功--上传
                    curractor.popFunction();   //弹栈
                    curractor.sendPredsResult(new ActorTask(idd, true, isInSelf));  //给上级

                    if(!ss.isEmpty()){
                        State state = (State)((ActorTask) ss.peek()).getObject();
                        if(state instanceof StateT2_3 ||state instanceof StateT2_4)
                            curractor.processSameADPred();
                    }
                }
                //(id,null,true)--不做任何处理
            }
        }else{       //false--T3.preds'检查失败
            list.clear();
        }
    }
}
