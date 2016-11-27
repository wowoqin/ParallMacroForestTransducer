package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT1_1 extends StateT1 {

    protected StateT1_1(ASTPath path) {
        super(path);

    }

    public static State TranslateState(ASTPath path) {//重新创建T1-1
        return new StateT1_1(path);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((getLevel() == layer) && (tag.equals(_test))) {
            // 在 list 中添加检查成功的任务模型
            addWTask(new WaitTask(layer, true, tag));
//            System.out.println("T1-1 开始标签匹配，add(wt)");
        }
        return true;
    }
    /*
    * 在设置完返回的结果之后不满足的就可以删除了--所以在这里剩下的都是满足条件的：
    *   T1-1 在任何情况下都只有一个list：
    *              上传 (多个)-- 遇到上层结束标签，弹栈、上传整个list、栈顶再执行一次endDo
    *              输出 (一个)-- 输出list.get(0) && 清空list  (stack.size()==1 && mainActor)
    *
    * */

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        // T1-1 不需要等待其他信息--只有一个list
        if(getLevel() == layer && tag.equals(_test)){     //遇到自己的结束标签
//            System.out.print("T1-1遇到自己的结束标签，");
            if(!list.isEmpty()){
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
//                    System.out.println("T1-1是整个XPath--输出");
                    WaitTask wtask = (WaitTask)list.get(0);
                    wtask.output();
                }
//                else System.out.println("T1-1是后续path");
            }
            else System.out.println("T1-1没遇到匹配的开始标签");
        }else if (layer == getLevel() - 1) {   // 遇到上层结束标签
            // T1-5 时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack
            System.out.println("T1-1遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){   //上传
                System.out.println(this+" 的list中元素的个数："+list.size()+",list中的第一个元素："+list.get(0));
                WaitTask wtask = (WaitTask)list.get(0);
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{list.size(), wtask.getPathR()}, isInself));
                if(!ss.isEmpty()) {      // 弹完之后当前actor 所在的stack 为空了，则删除当前 actor
                    State state = (State)((ActorTask)(ss.peek())).getObject();
                    if(state instanceof StateT1_5){
                        //此处选择发送消息是因为返回的消息肯定还未处理--先处理返回的path结果
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }
                }
            }else{
                System.out.println("T1-1没遇到匹配的开始标签--发送 NF");
                //上传一个 NF--得告诉上级我没找见
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
            }
        }
        return true;
    }
    //T1-1不会接收到返回的任何结果
}

