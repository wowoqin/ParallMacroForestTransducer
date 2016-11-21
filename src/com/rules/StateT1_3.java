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
public class StateT1_3 extends StateT1 implements Cloneable {

    protected StateT1_3(ASTPath path) {
        super(path);
    }

    public static State TranslateState(ASTPath path) {//重新创建T1-3
        return new StateT1_3(path);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {//当前层数大于等于应该匹配的层数 getLayer（）就可以
            addWTask(new WaitTask(layer, true, tag));
            System.out.println("T1-3 开始标签匹配，add(wt)");
        }
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        // T1-3 不需要等待--只有一个list
        if(tag.equals(_test)){//遇到自己的结束标签
            System.out.print("T1-3遇到自己结束标签，");
            if(!list.isEmpty()){
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.println("&& T1-3是整个XPath--输出最后一个标签 list.size= "+ list.size());
                    WaitTask wtask = (WaitTask)list.get(list.size()-1);  //每次输出最后一个元素
                    curactor.output(wtask);
                    list.remove(wtask);
                }else
                    System.out.println("&& T1-3是个后续path--等待上传");
            }else
                System.out.println("&& T1-3未找到匹配的开始标记");
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签--遇到的是最开始T1-3的上层结束标签
            // T1-5 时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack
            System.out.println("T1-3遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                int num = list.size();
                WaitTask wtask = (WaitTask)list.get(0);
                //传递整个list，pop（T1-3）
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{num, wtask.getPathR()}, isInself));
                if(!ss.isEmpty()){
                    task = (ActorTask)(ss.peek());
                    State currstate = (State)task.getObject();
                    if(currstate instanceof StateT1_5){
                        //此处选择发送消息是因为返回的消息肯定还未处理--先处理返回的path结果
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }else if(currstate instanceof StateT1_3){
                        //T1-3作为 AD 轴test的后续path，即T1-7/T1-8
                        curactor.processSameADPath(new Object[]{task.getId(),num,wtask});
                    }
                }
            }else{
                System.out.println("T1-3未找到匹配的开始标记--上传NF");
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
            }
        }
        return true;
    }
}