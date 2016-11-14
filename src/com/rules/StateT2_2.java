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
public class StateT2_2 extends StateT2 {
    protected State _q3;

    protected  StateT2_2(ASTPreds preds,State q3){
        super(preds);
        _q3 = q3;
    }

    public static StateT2 TranslateState(ASTPreds preds){//重新创建T2-2
        State q3 = StateT3.TranslateStateT3(preds.getFirstStep().getPreds());
        return new StateT2_2(preds,q3);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((layer==getLevel()) && (tag.equals(_test))){     // T2-2 的test匹配
            addWTask(new WaitTask(layer, null, true));
            _q3.setLevel(layer + 1);
            curactor.pushTaskDo(new ActorTask(layer, _q3, true));   //确定是给自己的
        }
        return true;
    }

    /* 到自己的结束标签的时候还是应该等一会呢--
    *      因为每次的返回结果针对的都是最后一个 wt，所以到自己的结束标签若pred检查失败，删除当前wt
    *      --保证list中只有一个 wt (id,null,true)---T2-2
    *      --保证list中只有两个 wt (id,null,null),(id,null,true)---T3-2
    * */

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
       /* 遇到自己的结束标签 && 进入endElementDo操作:
          1. 该actor之前的消息肯定已经处理完毕，不存在还在等待T2-2.q3结果的情况：因为 T2-2.q3 压在 T2-2的上面-->同一个actor
          2. 自己能遇到上层结束标签，谓词检查失败，弹栈 && remove 等待当前栈顶T2-2结果的 wt  */
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if(tag .equals(_test)){  //遇到自己的结束标签


        } else if (atask.getId() == getLevel() - 1) {   //上层结束标签--检查失败
            Stack ss = curactor.getMyStack();
            ActorTask task = ((ActorTask) ss.peek());   //(id,T2-2,isInself)
            int idd = task.getId();
            boolean isInSelf = task.isInSelf();
            //pop(T2-2)
            curactor.popFunction();
            //发消息（id,false,isInself）
            curactor.sendPredsResult(new ActorTask(idd, false, isInSelf));
            //当前栈不为空，栈顶进行endElementDo ：输出/上传/remove-->（T1-2或者T1-6）
            if (!ss.isEmpty()) {
                State state=((State) (((ActorTask) ss.peek()).getObject()));
                // T1-2 、T1-6的结束标签
                if(state instanceof StateT1_2 || state instanceof StateT1_6){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }
//            else {
//                actors.remove(curactor);
//                actorManager.detachActor(curactor);
//            }
        }
        return true;
    }

    /*
    * 收到谓词的返回结果：
    * 找到T2-2.list的中最后一个元素，设置
    * --若当前谓词满足了，还应该向上传递！！！
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(list.size()-1);//最后一个元素
        if(pred){    //true
            if(atask.isInSelf()){  //来自自己
                wt.setPredR(pred);
            }else{   //来自T3.preds'
                wt = (WaitTask)list.get(0);
                wt.setPathR(pred);
            }

            {
                Stack ss=curractor.getMyStack();
                ActorTask task = ((ActorTask) ss.peek());//(id,T2-2,isInself)
                int idd = task.getId();
                boolean isInSelf = task.isInSelf();


                if(wt.isPredsSatisified()) {   //(id,true,true)--上传
                    if(list.size() > 1){
                        curractor.sendPredsResult(new ActorTask(idd, true, true));   //给自己
                    } else {
                        curractor.popFunction(); //弹栈
                        curractor.sendPredsResult(new ActorTask(idd, true, isInSelf));  //给上级
                    }
                }
                else if(wt.isWaitT3ParallPreds()) { //(id,true,null)--(id,T2-2,isInself)换为（id,qw,isInself）
                    curractor.popFunction(); //弹栈
                    WaitState waitState=new WaitState();
                    waitState.setLevel(((State) atask.getObject()).getLevel());
                    waitState.list.add(wt);
                    curractor.getMyStack().push(new ActorTask(idd, waitState, isInSelf));
                }
            }

        }else{     //false
            if(atask.isInSelf()){
                list.remove();
            }else
                list.clear();
        }
    }
}
