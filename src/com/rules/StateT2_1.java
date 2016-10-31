package com.rules;

import com.XPath.PathParser.ASTPreds;
import com.actormodel.TaskActor;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT2_1 extends StateT2 {

    protected StateT2_1(ASTPreds preds) {
        super(preds);
    }

    public static StateT2 TranslateState(ASTPreds preds) {//重新创建T2-1
        return new StateT2_1(preds);
    }

    @Override
    public void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException { // layer 是当前 tag 的层数
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if(layer==getLevel() && tag.equals(_test)){
            ActorTask task = (ActorTask)curactor.getMyStack().peek();//栈顶
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();

            if(!list.isEmpty()){  //T3-1
                WaitState waitState=new WaitState();
                waitState.setLevel(((State) atask.getObject()).getLevel());
                waitState.list.add(this.list.get(0));
                //(id,T2-1,isInself) 换为 （id,qw,isInself）
                curactor.popFunction();
                curactor.pushTaskDo(new ActorTask(idd, waitState, isInSelf));
                //设置 T3-1.q'''检查成功（发消息而不是直接设置pred：是因为万一q''已经是检查成功的了呢）
                curactor.sendPredsResult(new ActorTask(id, true, true));//确定是给自己的--当前栈顶已经是 qw
            }else{  //T2-1
                //发送谓词结果 && pop 当前栈顶
                curactor.popFunction();
                curactor.sendPredsResult(new ActorTask(idd, true, isInSelf));
                if(curactor.getMyStack().isEmpty()){
                    actors.remove(curactor);
                    actorManager.detachActor(curactor);
                }
            }
        }
    }

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        if (atask.getId() == getLevel() - 1) {//遇到上层结束标签--检查失败
            Stack ss = curactor.getMyStack();
            ActorTask task = ((ActorTask) ss.peek());//栈顶(id,T2-1,isInself)
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();
            //pop(T2-1)
            curactor.popFunction();
            //发消息（id,false,isInself）
            curactor.sendPredsResult(new ActorTask(idd,false, isInSelf));
            //其实在此处还应该看T3-1.q''还在做检查否？是-->terminate

            //当前栈不为空，栈顶为 T1-2或者T1-6-->进行endElementDo 操作:输出/上传/remove/等待
            if (!ss.isEmpty()) {
                State state=((State) (((ActorTask) ss.peek()).getObject()));
                // T1-2 、T1-6的结束标签
                if(state instanceof StateT1_2 || state instanceof StateT1_6){
                    System.out.println(curactor.getName()+" 的栈顶是 "+state);
                    state.endElementDo(index,id,atask,curactor);
                }
            }else {
                actors.remove(curactor);
                actorManager.detachActor(curactor);
            }
        }
    }

    /*
    * 收到谓词的返回结果：证明原来是个T3-1--等待
    * 找到T2-1.list的中最后一个元素，设置
    * --若当前谓词满足了，还应该向上传递！！！
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(0);    //q==T3-1时，只有一个wt(id,null,null)
        if(pred){    //true
            if(atask.isInSelf()){  //来自自己--T2-1检查成功
                wt.setPredR(pred);
            }else{   //来自T3.preds'
                wt = (WaitTask)list.get(0);
                wt.setPathR(pred);
            }
            //设置完检查当前wt的表现形式
            {
                Stack ss=curractor.getMyStack();
                ActorTask task = ((ActorTask) ss.peek());//(id,T2-2,isInself)
                int idd = task.getId();
                boolean isInSelf = task.isInSelf();

                if(wt.isPredsSatisified()) {   //(id,true,true)--T3-1检查成功--上传
                    curractor.popFunction();   //弹栈
                    curractor.sendPredsResult(new ActorTask(idd, true, isInSelf));  //给上级
                }
                else if(wt.isWaitT3ParallPreds()) { //(id,true,null)--(id,T2-1,isInself)换为（id,qw,isInself）
                    curractor.popFunction(); //弹栈
                    WaitState waitState=new WaitState();
                    waitState.setLevel(((State) atask.getObject()).getLevel());
                    waitState.list.add(wt);
                    curractor.getMyStack().push(new ActorTask(idd, waitState, isInSelf));
                }
                //(id,null,true)--不做任何处理
            }
        }else{     //false
            if(atask.isInSelf()){
                list.remove();
            }else
                list.clear();
        }
    }
}




