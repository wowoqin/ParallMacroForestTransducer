package com.rules;

import com.XPath.PathParser.ASTPath;
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
    public  boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
            // 在 list 中添加需要等待匹配的任务模型
            addWTask(new WaitTask(layer, null, tag));
            System.out.print("T1-4 开始标签匹配，add(wt),wt.id= "+layer);
            String name = ((Integer) this.hashCode()).toString().concat("T1-4.prActor");
            Actor actor;
            ActorTask aatask;

            if(!actors.containsKey(name)) {   // 若predstack 为空
                System.out.println("，T1-4.prActor == null，创建");
                actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q3.setLevel(layer + 1);
                dmessage = new DefaultMessage("res&&push",
                        new Object[]{this._predstack,new ActorTask(layer, new Object[]{_q3,index,id}, false)});
                actorManager.send(dmessage, curactor, actor);
            }else{  // 若谓词 actor 已经创建了,则发送 q' 给 prActor即可
                System.out.print("，T1-4.prActor 已经存在");
                actor = actors.get(name);
                State currQ = (State) _q3.copy();
                currQ.setLevel(layer + 1);
                currQ.list = new ArrayList();
                aatask = new ActorTask(layer,new Object[]{currQ,index,id},false);
                if(!_predstack.isEmpty()){      //上一个的谓词已经检查成功弹栈了
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
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if (tag.equals(_test)) {     //遇到自己的结束标签，检查自己的list中的最后一个 wt -->输出/remove
            System.out.print("T1-4遇到自己结束标签，");
            if(!list.isEmpty()){
                WaitTask wtask = (WaitTask)list.get(list.size()-1);
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.print("T1-4是整个XPath，需要找到最后一个wt来对其判断，");
                    if(wtask.hasReturned()){
                        System.out.print("T1-4谓词结果已处理完毕,");
                        if(wtask.getPredR()){
                            System.out.println("满足--输出&删除最后一个wt");
                            wtask.output();
                        }else{
                            System.out.println("不满足--删除");
                        }
                        list.remove(wtask);  //(id,true/false,tag)
                    } else {
                        if(curactor.getMessageCount() > 0){
                            System.out.print("T1-4谓词已有返回结果，还未处理,");
                        }else{
                            System.out.println("T1-4谓词还没返回结果||返回结果还未处理,等啊等。。。");
                            do{
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while(curactor.getMessageCount() == 0);
                        }

                        System.out.println("T1-4谓词返回结果了--先去处理 predR");
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage,curactor,curactor);
                        return false; //中断此次处理--先处理返回的结果
                    }
                }else{
                    System.out.println("T1-4 是个后续path--查看最后一个 wt 的当前状态");
                    if(!wtask.hasReturned()){
                        System.out.println("T1-4 最后一个 wt 还未对predR进行处理 || predR还未返回");
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage,curactor,curactor);
                        return false; //中断此次处理--先处理返回的结果
                    }else if(!wtask.isSatisfiedOut()){
                        System.out.println("predR==false,删除");
                        list.remove(wtask);
                    }
                }
            }else{
                System.out.println("T1-4未找到匹配的开始标记 || 谓词返回false");
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
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{num, wtask}, isInself));
                if(!ss.isEmpty()){
                    task = (ActorTask)(ss.peek());
                    State currstate =(State)task.getObject();
                    if(currstate instanceof StateT1_5){
                        //此处选择发送消息是因为返回的消息肯定还未处理--先处理返回的path结果
                        dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                        actorManager.send(dmessage, curactor, curactor);
                        return false;
                    }else if(currstate instanceof StateT1_4){
                        //T1-4作为AD轴test的后续path，即T1-7/T1-8
                        curactor.processSameADPath(new Object[]{task.getId(),num,wtask});
                    }
                }
            }else{
                System.out.println("T1-4未找到匹配标记--上传NF");
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
            }
        }
        return true;
    }
    /*
    * 收到谓词的返回结果：
    * 找到T1-4.list中相应的元素，设置
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        System.out.println("T1-4 对返回的predR进行处理，到结束标记的时候，true-输出/false-删除,predR == " + pred);

        for(int i=list.size()-1;i>=0;i--){
            WaitTask wt = (WaitTask)list.get(i);  //id相同的元素
            if(wt.getId() == atask.getId()){
                wt.setPredR(pred);   //true/false先设置--到结束标记的时候，false的删除
                return;
            }
        }
    }

}


