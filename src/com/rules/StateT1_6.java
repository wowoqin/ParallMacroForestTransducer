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
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((getLevel() == layer)  && (tag.equals(_test))){
            System.out.print("T1-6匹配到开始标签，add(wt) && q3压栈，");
            list.add(new ArrayList<WaitTask>());
            addWTask(new WaitTask(layer, null, null));
            _q3.setLevel(layer + 1);
            this.getIndexAndId(index,id);

            curactor.pushFunction(new ActorTask(layer, _q3, true,index,id));
            String name=((Integer)curactor.hashCode()).toString().concat("T1-6.paActor");
            Actor actor;

            if(!actors.containsKey(name)){
                System.out.println("pathactor == null，创建了 q1 再压栈");
                actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q1.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",new ActorTask(this._pathstack,layer,_q1,false,index,id));
                actorManager.send(dmessage, curactor, actor);
            }else{  // 若path  actor 已经创建了,则发送 q'' 给 paActor即可
                System.out.println("pathactor 存在" );
                actor = actors.get(name);
                State currQ=(State)_q1.copy();
                currQ.setLevel(layer + 1);
                currQ.list = new ArrayList();
                ActorTask  aatask = new ActorTask(layer,currQ,false,index,id);
                if(!_pathstack.isEmpty()){      //上一个的path已经检查成功弹栈了
                    System.out.println("，pathstack 不为空，当前q1会add到curractor的缓存list中去");
                    dmessage = new DefaultMessage("needModifyIndex", aatask);
                    actorManager.send(dmessage, curactor, actor);
                    return true;
                }else{
                    System.out.println("，pathstack为空-即上一个q1已经检查成功弹栈了，当前q1直接压栈");
                    dmessage = new DefaultMessage("push",aatask);
                    actorManager.send(dmessage, curactor, actor);
                }
            }
        }
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        //T1-6 遇到自己的结束标签，则证明T1-6.q3 已经检查完了 && 返回了检查结果，还需检查 T1-6.q1 是否还需要等待
        if (layer == getLevel() && tag.equals(_test)) {   // 遇到自己的结束标签，检查
            if(!list.isEmpty()){    //至少还是有结果的
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                 //若是要输出，则list中只有一个list
                    System.out.print("T1-6是个XPath，");
                    ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(0);
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println("T1-6的path结果已处理完毕--输出");
                            for(WaitTask wwtask:llist){
                                curactor.output(wwtask);
                            }
                            list.remove(0);   //删除这个llist
                        }else{//还未处理返回结果
                            System.out.print("T1-6谓词/path还没返回结果||返回结果还未处理,");
                            do{
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while(curactor.getMessageCount() == 0);

                            System.out.println("T1-6谓词/path返回结果了--先处理 predR/pathR，当前结束标签重新入消息队列");
                            dmessage = new DefaultMessage("nodeID",new ActorTask(index,id));
                            actorManager.send(dmessage,curactor,curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-6.pred/path返回了检查失败的结果");
                        list.remove(0);   //删除这个为空的llist
                    }
                }else{ //肯定是要上传的，但是若此时还未处理path的返回结果，就该等待先处理--最后一个llist中的元素
                    System.out.println("T1-6是后续path--检查最后一个llist是否处理了返回结果");
                    ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(list.size()-1);//最后一个llist
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            System.out.println("T1-6谓词/path还没返回结果||返回结果还未处理,");

                            while(curactor.getMessageCount() == 0){
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

//                            System.out.println("T1-6谓词/path返回结果了--先处理 predR/pathR，当前结束标签重新入消息队列");
                            dmessage = new DefaultMessage("nodeID",new ActorTask(index,id));
                            actorManager.send(dmessage,curactor,curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-6.pred/path返回了检查失败的结果");
                        list.remove(llist);   //删除这个为空的llist
                    }
                }
            }else{
                System.out.println("T1-6未找到匹配的开始标记");
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
                for(int i=0;i<list.size();i++){     //所有不空的子llist的长度
                    ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(i);
                    if(!llist.isEmpty()){
                        num += llist.size();//上传的数量
                        if(wtask == null)
                            wtask = llist.get(0);
                    }
                }

                if(num > 0){
                    curactor.sendPathResult(new ActorTask(task.getId(),new Object[]{num,wtask.getPathR()},isInself));
                }else {
                    System.out.println("T1-6 path/pred检查失败，上传NF");
                    curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
                }
            }else{
                System.out.println("T1-6 没遇到其开始标签，上传NF");
                curactor.sendPathResult(new ActorTask(task.getId(), new Object[]{0, "NF"}, isInself));
            }
            //返回结果之后pop（T1-6），看当前栈顶
            if(!ss.isEmpty()){
                State currstate = (State)((ActorTask)ss.peek()).getObject();
                if(currstate instanceof StateT1_5){
                    dmessage = new DefaultMessage("nodeID",new ActorTask(index,id));
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }
        }
        return true;
    }

    /*
    * 收到谓词的返回结果：
    * 找到T1-6.list的中最后一个llist，设置
    * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        if(!list.isEmpty()){
            Boolean pred = (Boolean)atask.getObject();
            System.out.print("T1-6处理predR，");
            ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(list.size()-1);
            if(pred){
                System.out.println("preR==true，设置 llist 中所有 wt ");
                for(WaitTask wt:llist)
                    wt.setPredR(pred);//true--设置
            } else {
                System.out.println("preR==false，清空llist");
                llist.clear();  //false--清空当前llist
                //告诉path不用继续检查了--其实就是让path弹栈-若有等待压栈的就压栈
            }
        }
    }

    /*处理返回的path结果
   *   atask={num,tag}
   *   此时T1-5的最后一个等待list中只有一个wt，则需要copy num-1 份
   *
   * */
    @Override
    public void pathMatchFunction(ActorTask atask) {
        if(!list.isEmpty()){
            ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(list.size() - 1);//最后一个list
            System.out.print("T1-6 处理pathR，");

            if(!llist.isEmpty()){
                Object[] obj = (Object[])atask.getObject();
                int num = (Integer)obj[0];

                if(num==0){
                    System.out.println("pathR==notFound，清空llist");
                    llist.clear();   //清空llist
                } else {
                    System.out.println("返回了 "+num+" 个pathR，对llist进行设置");
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
}
