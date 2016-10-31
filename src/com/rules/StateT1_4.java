package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT1_4 extends StateT1 implements Cloneable {
    protected State _q3;//检查 preds

    protected StateT1_4(ASTPath path, State q3) {
        super(path);
        _q3 = q3;
        this._predstack = new Stack();
    }

    public static State TranslateState(ASTPath path) {//创建T1-4
        State q3 = StateT3.TranslateStateT3(path.getFirstStep().getPreds());
        return new StateT1_4(path, q3);
    }

    @Override
    public  void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
            // 在 list 中添加需要等待匹配的任务模型
            addWTask(new WaitTask(layer, null, tag));
            String name = ((Integer) this._predstack.hashCode()).toString().concat("T1-4.prActor");

            if(this._predstack.isEmpty()) {// 若predstack 为空
                Actor actor=actorManager.createAndStartActor(TaskActor.class, name);
                _q3.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",new Object[]{this._predstack,new ActorTask(layer, _q3, false)});
                actorManager.send(dmessage, curactor, actor);
            }else{  // 若谓词 actor 已经创建了,则发送 q' 给 prActor即可
                Actor actor = actors.get(name);
                State currQ=(State) _q3.copy();
                currQ.setLevel(layer + 1);
                dmessage=new DefaultMessage("push",new ActorTask(layer,currQ,false));
                actorManager.send(dmessage,curactor,actor);
            }
        }
    }

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor) {
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        if (tag.equals(_test)) {//遇到自己的结束标签，检查自己的list中的最后一个 wt -->输出/remove
            System.out.println("T1-4遇到自己结束标签");
            if(!list.isEmpty()){
                WaitTask wtask = (WaitTask)list.get(0);
                if(wtask.hasReturned()){
                    System.out.println("T1-4谓词结果已处理完毕");
                    if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                        System.out.println("T1-4是整个XPath");
                        if(wtask.isSatisfiedOut())
                            curactor.output(wtask);
                        else list.remove();  //(id,false,tag)
                    }
                }else{
                    System.out.println("T1-4谓词返回结果还未处理--需要先处理谓词返回结果");
                    //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                    if(curactor.getMessageCount() > 0){
                        DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(message,curactor,curactor);
                        return; //中断此次处理--先处理返回的结果
                    }else{
                        System.out.println("T1-4谓词还未返回结果");
                    }
                }
            }else{
                System.out.println("T1-4未找到匹配标记");
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签
            // T1-5 时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack
            System.out.println("T1-4遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                int num = getList().size();
                WaitTask wtask = (WaitTask)list.get(0);
                curactor.sendPathResult(new ActorTask(wtask.getId(), new Object[]{num, wtask}, isInself));
                if(!ss.isEmpty()){
                    task=(ActorTask)(ss.peek());
                    State currstate =(State)task.getObject();
                    if(currstate instanceof StateT1_5){
                        currstate.endElementDo(index,id,atask,curactor);
                    }else if(currstate instanceof StateT1_4){
                        //T1-4作为AD轴test的后续path，即T1-7/T1-8
                        curactor.processSameADPath(new Object[]{num,wtask});
                    }
                }else{
                    actors.remove(curactor.getName());
                    actorManager.detachActor(curactor);
                }
            }else{
                System.out.println("T1-4未找到匹配标记");
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }
        }
    }
    /*
    * 收到谓词的返回结果：
    * 找到T1-4.list的中最后一个元素，设置
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        WaitTask wt = (WaitTask)list.get(list.size()-1);//最后一个元素
        wt.setPredR(pred);   //true/false先设置--到结束标记的时候，false的删除
    }


}


