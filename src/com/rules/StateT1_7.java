package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

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

    @Override
    public void addWTask(WaitTask wtask){
        this.list.add(new LinkedList<WaitTask>().add(wtask));
    }

    @Override
    public void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
            System.out.println("T1-7.startElementDo中");
            // 在 list 中添加需要等待匹配的任务模型
            addWTask(new WaitTask(layer,true,null));
            String name=((Integer)this._pathstack.hashCode()).toString().concat("T1-7.paActor");

            if(this._pathstack.isEmpty()){  // 若pathActor 还没有创建 --> _pathstack 一定为空
                System.out.println("T1-7.test匹配 && pathactor == null");
                Actor actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q1.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",new Object[]{this._pathstack,new ActorTask(layer, _q1, false)});
                actorManager.send(dmessage, curactor, actor);
            } else{  // 若path  actor 已经创建了,则发送 q'' 给 paActor即可
                System.out.println("T1-7.test匹配 && pathactor != null");
                Actor actor=actors.get(name);
                State currQ=(State)_q1.copy();
                currQ.setLevel(layer + 1);
                dmessage=new DefaultMessage("push",new ActorTask(layer,currQ,false));
                actorManager.send(dmessage, curactor, actor);
            }
        }
    }

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if (tag.equals(_test)) {  // 遇到自己的结束标签，检查
            if(!list.isEmpty()){
                LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size()-1);
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    //若是要输出，则输出list中最后一个list
                    System.out.println("T1-7是个XPath");
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println("T1-7的path结果已处理完毕");
                            for(WaitTask wwtask:llist){
                                curactor.output(wwtask);
                            }
                        }else{   //还未处理返回结果
                            System.out.println("T1-7的path结果还未处理");
                            //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                            if(curactor.getMessageCount() > 0){
                                System.out.println("先去处理T1-7的path返回结果");
                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理--先处理返回的结果
                            }else{
                                System.out.println("T1-7的path还未返回结果");
                                while(curactor.getMessageCount()==0){
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message, curactor, curactor);
                                return; //中断此次处理--先处理返回的结果
                            }
                        }
                    }else{
                        System.out.println("T1-7未找到匹配标记 || path返回NF");
                        list.remove();     //删除为空的llist
                    }
                }else{ //肯定是要上传的，但是若此时还未处理path的返回结果，就该等待先处理--最后一个llist中的元素
                    System.out.println("T1-7是后续path");
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            System.out.println("T1-7的path结果还未处理");
                            //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                            if(curactor.getMessageCount() > 0){
                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理--先处理返回的结果
                            }else{
                                System.out.println("T1-7的path还未返回结果，等待path的结果处理了再继续扫描！");
                                while(curactor.getMessageCount()==0){
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message, curactor, curactor);
                                return; //中断此次处理--先处理返回的结果
                            }
                        }
                    }else{
                        System.out.println("T1-7未找到匹配标记 || path返回NF");
                        list.remove();     //删除为空的llist
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
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(i);
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
                        }else if(currstate instanceof StateT1_7){
                            //T1-7作为AD轴test的后续path，即T1-7/T1-8
                            curactor.processSameADPath(new Object[]{num,wtask});
                        }
                    }else{
                        actors.remove(curactor.getName());
                        actorManager.detachActor(curactor);
                    }
                    return;
                }else {
                    System.out.println("T1-7 path检查失败，无上传结果");
                    curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
                }
            }else{
                System.out.println("T1-7没遇到其开始标记&&遇到了上层结束标记，无上传结果--NF");
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }

            if(!ss.isEmpty()){
                task = (ActorTask)(ss.peek());
                State currstate = (State)task.getObject();
                if(currstate instanceof StateT1_5){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                }
            }else{
                actors.remove(curactor.getName());
                actorManager.detachActor(curactor);
            }
        }
    }

    /*处理返回的path结果
  *   atask={num,tag}
  *   此时T1-7的最后一个等待list中只有一个wt，则需要copy num-1 份
  *
  * */
    @Override
    public void pathMatchFunction(ActorTask atask) {
        LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size() - 1);//最后一个list

        if(!llist.isEmpty()){
            Object[] obj = (Object[])atask.getObject();
            int num = (Integer)obj[0];

            if(num==0){
                llist.clear();   //清空llist
            } else {
                WaitTask wt = llist.get(0);
                String tag = (String) obj[1];
                wt.setPathR(tag);
                for(int i = 0;i<num - 1;i++)
                    llist.add(wt);
            }
        }
        //else--已经是空的（即pred返回false），若输出--删除空的llist、、若上传，计算的时候size==0
    }
}
