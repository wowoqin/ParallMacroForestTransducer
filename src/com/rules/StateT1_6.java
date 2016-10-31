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
public class StateT1_6 extends StateT1{
    protected State _q3;//检查 preds
    protected State _q1;//检查后续 path
    protected StateT1_6(ASTPath path,State q3,State q1){
        super(path);
        _q3=q3;
        _q1=q1;
        _pathstack =new Stack();
    }

    public static State TranslateState(ASTPath path){//重新创建T1-6
        State q3=StateT3.TranslateStateT3(path.getFirstStep().getPreds());
        State q1=StateT1.TranslateStateT1(path.getRemainderPath());
        return new StateT1_6(path,q3,q1);//然后压入栈
    }
    /*
   * T1-6--每一个T1-6都有多个匹配结果，所以每一个开始标签一个list
   * -- 若输出：list中只有一个list
   * -- 若上传：list中多个list
   * -- 返回给T1-6的结果的匹配都是相对于其最后一个list而言 && 结果返回了之后就检查淘汰--上传的时候就不用检查了
   *  */

    @Override
    public void addWTask(WaitTask wtask){
        this.list.add(new LinkedList<WaitTask>().add(wtask));
    }

    @Override
    public void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((getLevel() == layer)  && (tag.equals(_test))){
            System.out.println("T1-6.startElementDo中");
            // 在 list 中添加需要等待匹配的任务模型
            addWTask(new WaitTask(layer, null, null));
            _q3.setLevel(layer + 1);
            curactor.pushTaskDo(new ActorTask(layer, _q3, true));

            String name=((Integer)this._pathstack.hashCode()).toString().concat("T1-6.paActor");
            if(this._pathstack.isEmpty()){  // 若_pathstack 为空
                System.out.println("T1-6.test匹配 && pathactor == null");
                Actor actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q1.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",new Object[]{this._pathstack,new ActorTask(layer, _q1, false)});
                actorManager.send(dmessage, curactor, actor);
            }else{  // 若path  actor 已经创建了,则发送 q'' 给 paActor即可
                System.out.println("T1-6.test匹配 && pathactor != null" );
                Actor actor = actors.get(name);
                State currQ=(State)_q1.copy();
                currQ.setLevel(layer+1);
                dmessage=new DefaultMessage("push", new ActorTask(layer,currQ,false));
                actorManager.send(dmessage, curactor, actor);
            }
        }
    }

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        //T1-6 遇到自己的结束标签，则证明T1-6.q3 已经检查完了 && 返回了检查结果，还需检查 T1-6.q1 是否还需要等待
        if (tag.equals(_test)) {// 遇到自己的结束标签，检查
            if(!list.isEmpty()){  //至少还是有结果的
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                 //若是要输出，则list中只有一个list
                    System.out.println("T1-6是个XPath");
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(0);
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println("T1-6的path结果已处理完毕");
                            for(WaitTask wwtask:llist){
                                curactor.output(wwtask);
                            }
                        }else{//还未处理返回结果
                            System.out.println("T1-6的path结果还未处理");
                            //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                            if(curactor.getMessageCount() > 0){
                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理 -- 先处理返回的结果
                            }else{
                                System.out.println("T1-6的path还未返回结果");
                                while(curactor.getMessageCount()==0){
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理--先处理返回的结果
                            }
                        }
                    }else{
                        System.out.println("T1-6未找到匹配标记");
                        list.remove();//删除这个为空的llist
                    }
                }else{ //肯定是要上传的，但是若此时还未处理path的返回结果，就该等待先处理--最后一个llist中的元素
                    System.out.println("T1-6是后续path");
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size()-1);//最后一个llist
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            System.out.println("T1-6的path结果还未处理");
                            //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                            if(curactor.getMessageCount() > 0){
                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理--先处理返回的结果
                            }else{
                                //存在这种情况：T1-6所在线程处理的的快，其path所在线程处理的慢，也许还未处理完--当前等一会
                                System.out.println("T1-6的path还未返回结果");
                                //若现在不等待，万一下一个标签是上层结束标签--T1-6是要上传结果的&&不再做返回结果的检查工作
                                while(curactor.getMessageCount()==0){
                                    try {
                                        Thread.sleep(1);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                                DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                                actorManager.send(message,curactor,curactor);
                                return; //中断此次处理--先处理返回的结果
                            }
                        }
                    }
                }
            }else{
                System.out.println("T1-6未找到匹配标记");
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签(肯定是作为后续path)
            // (能遇到上层结束标签，即T1-6作为一个后续的path（T1-5 的时候也会放在stackActor中），T1-6~T1-8会被放在paActor中)
            // T1-5 的后续的path时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack中
            System.out.println("T1-6遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                int num = 0;
                WaitTask wtask = null;
                for(int i=0;i<list.size();i++){//所有不空的子llist的长度
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(i);
                    if(!llist.isEmpty()){
                        num += llist.size();//上传的数量
                        if(wtask == null)
                            wtask = llist.get(0);
                    }
                }

                if(num > 0){
                    curactor.sendPathResult(new ActorTask(0,new Object[]{num,wtask},isInself));
                }else {
                    System.out.println("T1-6 path/pred检查失败，无上传结果");
                    curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
                }
            }else{
                System.out.println("T1-6没遇到其开始标签，无上传结果");
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }
            //返回结果之后pop（T1-6），看当前栈顶
            if(!ss.isEmpty()){
                task = (ActorTask)(ss.peek());
                State currstate = (State)task.getObject();
                if(currstate instanceof StateT1_5)
                    currstate.endElementDo(index, id, atask, curactor);
            }else{
                actorManager.detachActor(curactor);
            }
        }
    }

    /*
    * 收到谓词的返回结果：
    * 找到T1-6.list的中最后一个llist，设置
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size()-1);
        if(pred){
            for(WaitTask wt:llist)
                wt.setPredR(pred);//true--设置
        } else {
            llist.clear();  //false--清空当前llist
            //告诉path不用继续检查了--
        }
    }

    /*处理返回的path结果
   *   atask={num,tag}
   *   此时T1-5的最后一个等待list中只有一个wt，则需要copy num-1 份
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
