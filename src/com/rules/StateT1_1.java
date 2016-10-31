package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
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
    public void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((getLevel() == layer) && (tag.equals(_test))) {
            // 在 list 中添加检查成功的任务模型
            addWTask(new WaitTask(layer, true, tag));
        }
    }
    /*
    * 在设置完返回的结果之后不满足的就可以删除了--所以在这里剩下的都是满足条件的：
    *              上传 -- 遇到上层结束标签，弹栈、上传、删除最后一个list + 栈顶再执行一次endDo
    *              输出 -- stack.size()==1 && mainActor :输出 && 删除最后一个list
    *
    * */

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        // T1-1 不需要等待其他信息--只有一个list
        if(tag.equals(_test)){     //遇到自己的结束标签
            System.out.println("T1-1遇到自己结束标签");
            if(!list.isEmpty()){
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.println("T1-1是整个XPath");
                    WaitTask wtask = (WaitTask)list.get(0);
                    curactor.output(wtask);
                }
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签
            // T1-5 时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack
            System.out.println("T1-1遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            int idd = task.getId();
            boolean isInself = task.isInSelf();
            if(!list.isEmpty()){ //上传
                WaitTask wtask = (WaitTask)list.get(0);
                curactor.sendPathResult(new ActorTask(idd, new Object[]{list.size(), wtask}, isInself));
                if(ss.isEmpty()) {      // 弹完之后当前actor 所在的stack 为空了，则删除当前 actor
                    actors.remove(curactor.getName());
                    actorManager.detachActor(curactor);
                }else{                      // T1-1 作为 T1-5 的后续 path
                    State state =(State)((ActorTask)(ss.peek())).getObject();
                    if(state instanceof StateT1_5)
                        state.endElementDo(index,id,atask,curactor);
                }
            }else{
                System.out.println("T1-1未找到匹配标记");
                //上传一个 NF--得告诉上级我没找见
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }
        }
    }
    //T1-1不会接收到返回的任何结果
}

