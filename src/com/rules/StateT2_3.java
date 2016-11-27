package com.rules;

import com.XPath.PathParser.ASTPreds;
import com.actormodel.TaskActor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT2_3 extends StateT2{

    protected  StateT2_3(ASTPreds preds){
        super(preds);
    }

    public static StateT2 TranslateState(ASTPreds preds){//重新创建T2-3
        return new StateT2_3(preds);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {// layer 是当前 tag 的层数
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((layer >= getLevel()) && (tag.equals(_test))){
            Stack ss = curactor.getMyStack();
            ActorTask task = ((ActorTask) ss.peek());
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();

            if(!list.isEmpty()){    //T3-3 && T3-3.q'''检查成功
                System.out.println("T3-3.q'''检查成功,==>(id,T2-3,isInself)换为（id,qw,isInself）");
                WaitState waitState = new WaitState();
                waitState.setLevel(getLevel());
                waitState.getList().add(list.get(0));
                curactor.popFunction();
                //(id,T2-3,isInself) 换为 （id,qw,isInself）
                curactor.getMyStack().push(new ActorTask(idd, waitState, isInSelf));
                //设置 T3-3.q'''检查成功-->
                curactor.sendPredsResult(new ActorTask(idd, true, true));//确定是给自己的
                /*在谓词全部满足或者q''不满足弹栈的时候--> 栈顶为(id,qw,isInSelf)：
                 *  1. 满足：wt（layer,true,"true"）
                *   2. 不满足：wt（layer,true,"false"）
                *                          弹 qw 了之后：：：-->栈顶(id,T2-3,false)
                * */
            }else{  //T2-3
                //发送谓词结果 && pop 当前栈顶
                curactor.popFunction();
                curactor.sendPredsResult(new ActorTask(idd, true, isInSelf));
                if(!ss.isEmpty()){
                    //pop 完了之后还是T2-3 && T2-3 是要传给上级actor的
                    State state = (State)((ActorTask) ss.peek()).getObject();
                    if((state instanceof StateT2_3) && !isInSelf){   // T2-3 作为 AD 轴test的谓词
                        curactor.processSameADPred();
                    }
                }else if(!curactor.getMylist().isEmpty()){
                    curactor.processEmStackANDNoEmMylist();
                }
            }
        }
        return true;
    }

    //谓词要是能自己遇见上层结束标签，则表明自己是检查失败的
    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        // 自己能遇到上层结束标签，谓词检查失败，弹栈 && remove 等待当前栈顶T2-3结果的 wt
        if (atask.getId() == getLevel() - 1) {
            Stack ss=curactor.getMyStack();
            ActorTask task = ((ActorTask) ss.peek());//(id,T2-3,isInself)
            int idd = atask.getId();
            boolean isInSelf = task.isInSelf();
            //pop(id,T2-3,isInself)
            curactor.popFunction();
            //发消息（id,false,isInself）
            curactor.sendPredsResult(new ActorTask(idd, false, isInSelf));
            //当前栈不为空，栈顶进行endElementDo 操作（输出（T1-2或者T1-6）
            if (!ss.isEmpty()) {
                State state=((State) (((ActorTask) ss.peek()).getObject()));
                // T1-2 、T1-6的结束标签
                if(state instanceof StateT1_2 || state instanceof StateT1_6){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }else if(!curactor.getMylist().isEmpty()){
                curactor.processEmStackANDNoEmMylist();
            }
        }
        return true;
    }

    /*
    * 收到谓词的返回结果：证明原来是个T3-3--等待
    * 找到T2-3.list的中最后一个元素，设置
    * --若当前谓词满足了，还应该向上传递！！！
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(0);    //q==T3-3时，只有一个wt(id,null,null)
        if(pred){    //true
            if(atask.isInSelf()){  //来自自己--T2-3检查成功
                System.out.println("来自自己--T2-3检查成功");
                wt.setPredR(pred);
            }else{   //来自T3.preds'
                System.out.println("来自T3.preds' ");
                wt.setPathR(pred);
            }
            //设置完检查当前wt的表现形式
            {
                Stack ss = curractor.getMyStack();
                ActorTask task = ((ActorTask) ss.peek());//(id,T2-2,isInself)
                int idd = task.getId();
                boolean isInSelf = task.isInSelf();

                if(wt.isPredsSatisified()) {   //(id,true,true)--T3-3检查成功--上传
                    curractor.popFunction();   //弹栈
                    curractor.sendPredsResult(new ActorTask(idd, true, isInSelf));  //给上级
                    if(!ss.isEmpty() && ((ActorTask) ss.peek()).getObject() instanceof StateT2_3){
                        curractor.processSameADPred();
                    }
                }else if(wt.isWaitT3ParallPreds()) { //(id,true,null)--(id,T2-1,isInself)换为（id,qw,isInself）
                    curractor.popFunction(); //弹栈
                    WaitState waitState=new WaitState();
                    waitState.setLevel(((State) task.getObject()).getLevel());
                    waitState.list.add(wt);
                    curractor.getMyStack().push(new ActorTask(idd, waitState, isInSelf));
                }
                //(id,null,true)--不做任何处理
            }
        }else{     //false
            if(atask.isInSelf()){
                list.remove(list.size()-1);
            }else
                list.clear();
        }
    }
}

