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
public class StateT1_2 extends StateT1 {
    protected State _q3;//检查 preds

    protected StateT1_2(ASTPath path, State q3) {
        super(path);
        _q3 = q3;
    }

    public static State TranslateState(ASTPath path) {   //重新创建T1-2
        State q3 = StateT3.TranslateStateT3(path.getFirstStep().getPreds());
        return new StateT1_2(path, q3);
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {// layer 表示当前标签 tag 的层数
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((getLevel() == layer) && (tag.equals(_test))) {
            System.out.println("T1-2匹配到开始标签，压栈 & add(wt)");
            addWTask(new WaitTask(layer, null, tag));
            _q3.setLevel(layer + 1);
            curactor.pushTaskDo(new ActorTask(layer, new Object[]{_q3,index,id}, true));
        }
        return true;
    }
    /*
    * 在设置完返回的结果之后不满足的就可以删除了--所以在这里剩下的都是满足条件的：
    *   T1-1 在任何情况下都只有一个list：
    *              上传 (多个)-- 遇到上层结束标签，弹栈、上传整个list、栈顶再执行一次endDo
    *              输出 (一个)-- 输出list.get(0) && 清空list  (stack.size()==1 && mainActor)
    *   结束标记--检查谓词是否返回，若没：等待
    *
    *   因为在每一个标签进行处理的时候，首先检查messages.count >0否，若消息队列有消息，先处理返回的其他消息；
    *   早检查成功 --已处理
    *   刚检查成功 --已处理
    *   检查失败   --才返回false，肯定没处理
    * */

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if(layer == getLevel() && tag.equals(_test)){//遇到自己的结束标签--说明谓词已经弹栈
            System.out.println("T1-2遇到自己结束标签--谓词已弹栈-->已发回结果");
            if(!list.isEmpty()){
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.print("T1-2是整个XPath，");
                    WaitTask wtask = (WaitTask)list.get(0);
                    if(wtask.hasReturned()){
                        System.out.println("T1-2谓词结果已处理完毕--输出&删除");
                        curactor.output(wtask);
                        list.remove(0);
                    }else {// 谓词已经弹栈并返回了消息，在处理下一个标签之前会检查actor的消息队列的消息数，先处理返回结果
                        System.out.println("T1-2谓词返回结果还未处理--等待，先处理predR");
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }
                }else{
                    System.out.println("T1-2是后续path--需检查最后一个 wt 的谓词结果是否返回");
                    WaitTask wtask = (WaitTask)list.get(list.size()-1);
                    if(!wtask.hasReturned()){
                        System.out.println("T1-2谓词返回结果还未处理");
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }
                }
            }else{
                System.out.println("T1-2未找到匹配标记 || T1-2谓词返回false");
            }
        }else if (layer == getLevel() - 1) {   // 遇到上层结束标签
            // T1-5 时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack
            System.out.println("T1-2遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                WaitTask wtask = (WaitTask)list.get(0);
                //传回整个list，pop栈顶
                curactor.sendPathResult(new ActorTask(wtask.getId(), new Object[]{list.size(), wtask.getPathR()}, isInself));
                if(!ss.isEmpty()) {   // 弹完之后当前actor 所在的stack 为空了，则删除当前 actor
                    State state =(State)((ActorTask)(ss.peek())).getObject();
                    if(state instanceof StateT1_5){
                        //此处选择发送消息是因为返回的消息肯定还未处理--先处理返回的path结果
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }
                }
            }else{
                System.out.println("T1-2未找到匹配标记 || T1-2谓词返回false--发送 NF");
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
            }
        }
        return true;
    }
    /*
    * 收到谓词的返回结果：
    * 找到T1-2.list的中最后一个元素，设置
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt  = (WaitTask)list.get(list.size()-1);//最后一个元素
        if(pred){   //true
            System.out.println("T1-2对谓词返回结果 true 进行处理--wt等待输出吧");
            wt.setPredR(pred);
        }else{     //false
            System.out.println("T1-2对谓词返回结果 false 进行处理--remove(wt)");
            list.remove(list.size()-1);     //若是要输出--list不为空||若是要上传，留下的只是满足条件的
        }
    }
}
