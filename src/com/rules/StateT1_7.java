package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT1_7 extends StateT1 implements Cloneable{
    protected State _q1;//检查后续 path

    protected StateT1_7(ASTPath path, State q1) {
        super(path);
        _q1 = q1;
        this._pathstack=new Stack();
    }

    public static State TranslateState(ASTPath path) {  //重新创建T1-7
        State q1 = StateT1.TranslateStateT1(path.getRemainderPath());
        return new StateT1_7(path, q1);
    }
    /*
       * T1-7--每一个T1-7都有多个匹配结果，所以每一个开始标签一个list
       * -- 若输出：list中只有一个llist
       * -- 若上传：list中多个llist
       * -- 返回给T1-7的结果的匹配都是相对于其最后一个list而言 && 结果返回了之后就检查淘汰--上传的时候就不用检查了
       *  */
//
//    @Override
//    public void addWTask(WaitTask wtask){
//        list.add(new LinkedList<WaitTask>());
//        addWTask(wtask);
////        this.list.add(new LinkedList<WaitTask>().add(wtask));
//    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
            // 在 list 中添加需要等待匹配的任务模型
            System.out.print("T1-7.test匹配，add(wt)，");
            list.add(new ArrayList<WaitTask>());
            addWTask(new WaitTask(layer, true, null));
            String name = ((Integer)this.hashCode()).toString().concat("T1-7.paActor");
            Actor actor;
            ActorTask aatask = null;

            if(!actors.containsKey(name)){
                System.out.println("pathactor == null,创建后q1再压栈");
                actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q1.setLevel(layer + 1);
                dmessage = new DefaultMessage("res&&push",new Object[]{this._pathstack,new ActorTask(layer, _q1, false)});
                actorManager.send(dmessage, curactor, actor);
            } else{  // 若path  actor 已经创建了,则发送 q'' 给 paActor即可
                System.out.println(" pathactor != null，q1估计会add到curractor的缓存list中去");
                actor = actors.get(name);
                State currQ = (State)_q1.copy();
                currQ.list = new ArrayList();
                currQ.setLevel(layer + 1);
                aatask = new ActorTask(layer,currQ,false);
            }

            if(id == 1){
                System.out.println(" 当前数据块处理结束--要让 "+name+" 先去cacheActor那里取到 Index：++index");
                DefaultMessage message1 = new DefaultMessage("needModifyIndex", new Object[]{++index,0,aatask});
                actorManager.send(message1, curactor, actor);
            }else {
                System.out.println("当前数据块还没结束，currActor告诉 "+name+" 先去cacheActor那里取到 Index：index");
                dmessage = new DefaultMessage("needModifyIndex", new Object[]{index, ++id,aatask});
                actorManager.send(dmessage, curactor, actor);
            }

        }
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if (tag.equals(_test)) {  // 遇到自己的结束标签，检查
            if(!list.isEmpty()){
                ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(list.size()-1);
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    //若是要输出，则输出list中最后一个list
                    System.out.print("T1-7是个XPath，list.size()= "+list.size());
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println(",T1-7的path结果已处理完毕--输出");
                            for(WaitTask wwtask:llist){
                                curactor.output(wwtask);
                            }
                            list.remove(list.size()-1);   //删除这个llist
                        }else{   //还未处理返回结果
                            System.out.println(",T1-7 path还没返回结果||返回结果还未处理,等啊等。。。");
                            do{
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while(curactor.getMessageCount() == 0);

                            System.out.println("T1-7.path返回结果了--先去处理 pathR，当前结束标签重新入消息队列");
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage,curactor,curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-7.path返回NF，删除llist");
                        list.remove(llist);     //删除为空的llist
                    }
                }else{ //肯定是要上传的，但是若此时还未处理path的返回结果，就该等待先处理--最后一个llist中的元素
                    System.out.print("T1-7是后续path，检查最后一个llist，");
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            System.out.print("T1-7 path还没返回结果||返回结果还未处理,");
                            do{
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while(curactor.getMessageCount() == 0);

                            System.out.println("T1-7.path返回结果了--先去处理 pathR，当前结束标签重新入消息队列");
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage,curactor,curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-7 path 返回NF，llist为空，删除");
                        list.remove(llist);     //删除为空的llist
                    }
                }
            }else{
                System.out.println("T1-7未找到匹配的开始标记");
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签(肯定是作为后续path)
            System.out.println("T1-7遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                int num = 0;
                WaitTask wtask = null;
                for(int i=0;i<list.size();i++){
                    ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(i);
                    if(!llist.isEmpty()){
                        num += llist.size();//上传的数量
                        if(wtask == null)
                            wtask = llist.get(0);
                    }
                }

                if(num > 0){
                    curactor.sendPathResult(new ActorTask(0,new Object[]{num,wtask},isInself));
                    if(!ss.isEmpty()){
                        task = (ActorTask)(ss.peek());
                        State currstate = (State)task.getObject();
                        if(currstate instanceof StateT1_5){
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage, curactor, curactor);
                            return false;
                        }else if(currstate instanceof StateT1_7){
                            //T1-7作为AD轴test的后续path，即T1-7/T1-8
                            curactor.processSameADPath(new Object[]{num,wtask});
                        }
                    }
                }else {
                    System.out.println("T1-7 path检查失败，无上传结果");
                    curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
                }
            }else{
                System.out.println("T1-7没遇到其开始标记&&遇到了上层结束标记，上传结果 NF");
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }

            if(!ss.isEmpty()){
                task = (ActorTask)(ss.peek());
                State currstate = (State)task.getObject();
                if(currstate instanceof StateT1_5){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }
        }
        return true;
    }

    /*处理返回的path结果
  *   atask={num,tag}
  *   此时T1-7的最后一个等待list中只有一个wt，则需要copy num-1 份
  *
  * */
    @Override
    public void pathMatchFunction(ActorTask atask) {
        System.out.print("T1-7 处理pathR，");

        for(int i = list.size()-1;i >= 0;i--){
            ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(i);
            if(!llist.isEmpty()){
                WaitTask wt = llist.get(0);
                if(wt.getId() == atask.getId()){
                    Object[] obj = (Object[])atask.getObject();
                    int num = (Integer)obj[0];

                    if(num == 0){
                        System.out.println("pathR == notFound，清空llist");
                        llist.clear();   //清空llist
                    } else {
                        System.out.println("返回了 "+num+" 个pathR，对llist进行设置");

                        String tag = (String) obj[1];
                        wt.setPathR(tag);
                        for(int j = 0;j<num - 1;j++)
                            llist.add(wt);
                    }
                    return;
                }
            }
            //else--已经是空的（即pred返回false），若输出--删除空的llist、、若上传，计算的时候size==0
        }
    }
}
