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
public class StateT1_8 extends StateT1 {
    protected State _q3;//检查 preds
    protected State _q1;//检查后续 path


    protected StateT1_8(ASTPath path, State q3, State q1) {
        super(path);
        _q3 = q3;
        _q1 = q1;
        this._predstack = new Stack();
        this._pathstack = new Stack();
    }

    public static State TranslateState(ASTPath path) {//重新创建T1-8
        State q3 = StateT3.TranslateStateT3(path.getFirstStep().getPreds());
        State q1 = StateT1.TranslateStateT1(path.getRemainderPath());
        return new StateT1_8(path, q3, q1);//然后压入栈
    }

    /*
       * T1-8--每一个T1-8都有多个匹配结果，所以每一个开始标签一个list
       * -- 若输出：list中只有一个llist
       * -- 若上传：list中多个llist
       * -- 返回给T1-8的结果的匹配都是相对于其最后一个list而言 && 结果返回了之后就检查淘汰--上传的时候就不用检查了
       *  */
//    @Override
//    public void addWTask(WaitTask wtask){
//        this.list.add(new LinkedList<WaitTask>().add(wtask));
//    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if ((layer >= getLevel()) && (tag.equals(_test))) {
//            System.out.print("T1-8.test匹配，add(wt)，");
            list.add(new ArrayList<WaitTask>());
            addWTask(new WaitTask(layer, null, null));
            String name = ((Integer)this.hashCode()).toString().concat("T1-8.prActor");
            Actor actor;
            ActorTask aatask;

            if (!actors.containsKey(name)) {   // 若predstack 为空
                System.out.println("practor == null,先创建 prActor");
                actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q3.setLevel(layer + 1);
                dmessage = new DefaultMessage("res&&push",
                        new Object[]{this._predstack, new ActorTask(layer, new Object[]{_q3,index,id}, false)});
                actorManager.send(dmessage, curactor, actor);
                //向 actor 发送数据块的 index + id
                System.out.println(name + " 直接去cacheactor那里取数据块：++index/index");
                if(id == 1){
                    dmessage = new DefaultMessage("modifyIndex", new Object[]{index+1,0});
                    actorManager.send(dmessage, curactor, actor);
                }else {
                    dmessage = new DefaultMessage("modifyIndex", new Object[]{index, id+1});
                    actorManager.send(dmessage, curactor, actor);
                }
            } else {  // 若谓词 actor 已经创建了,则发送 q' 给 prActor即可
                System.out.print("predactor != null，");
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
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{index+1,0,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }else {
                        System.out.println("当前数据块还没结束，" + name + " 的Index：index");
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{index, id+1,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }
                }else{
                    System.out.println("，predstack为空-即上一个q3已经检查成功弹栈了，当前q3直接压栈");
                    dmessage = new DefaultMessage("push",aatask);
                    actorManager.send(dmessage, curactor, actor);
                    //向 actor 发送数据块的 index + id
                    System.out.println(name + " 直接去cacheactor那里取数据块：++index/index");
                    if(id == 1){
                        dmessage = new DefaultMessage("modifyIndex", new Object[]{index+1,0});
                        actorManager.send(dmessage, curactor, actor);
                    }else {
                        dmessage = new DefaultMessage("modifyIndex", new Object[]{index, id+1});
                        actorManager.send(dmessage, curactor, actor);
                    }
                }
            }



            name = ((Integer)(this.hashCode()+1)).toString().concat("T1-8.paActor");
            if(!actors.containsKey(name)){  // 若pathActor 还没有创建 --> _pathstack 一定为空
                System.out.println("pathactor == null，先创建 paActor");
                actor = actorManager.createAndStartActor(TaskActor.class, name);
                _q1.setLevel(layer + 1);
                dmessage=new DefaultMessage("res&&push",
                        new Object[]{this._pathstack,new ActorTask(layer, new Object[]{_q1}, false)});
                actorManager.send(dmessage, curactor, actor);
            } else{  // 若path  actor 已经创建了,则发送 q'' 给 paActor即可
                System.out.print("pathactor != null，");
                actor = actors.get(name);
                State currQ=(State)_q1.copy();
                currQ.setLevel(layer + 1);
                currQ.list = new ArrayList();
                aatask = new ActorTask(layer,new Object[]{currQ},false);
                if(!_pathstack.isEmpty()){      //上一个的path已经检查成功弹栈了
                    System.out.println("pathstack 不为空，当前q1会add到curractor的缓存list中去");
                    //向 actor 发送数据块的 index + id
                    if(id == 1){
                        System.out.println("当前数据块处理结束，" + name + " 的Index：++index");
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{++index,0,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }else {
                        System.out.println("当前数据块还没结束，" + name + " 的Index：index");
                        dmessage = new DefaultMessage("needModifyIndex", new Object[]{index, ++id,aatask});
                        actorManager.send(dmessage, curactor, actor);
                    }
                    return true;
                }else{
                    System.out.println("，pathstack为空-即上一个q1已经检查成功弹栈了，当前q1直接压栈");
                    dmessage = new DefaultMessage("push",aatask);
                    actorManager.send(dmessage, curactor, actor);
                }
            }

            //向 actor 发送数据块的 index + id
            System.out.println(name + " 直接去cacheactor那里取数据块：++index/index");
            if(id == 1){
                dmessage = new DefaultMessage("modifyIndex", new Object[]{++index,0});
                actorManager.send(dmessage, curactor, actor);
            }else {
                dmessage = new DefaultMessage("modifyIndex", new Object[]{index, ++id});
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
            if(!list.isEmpty()) {   //至少还是有结果的
                ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(list.size()-1);
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.print("T1-8是个XPath，");
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println("T1-8的path/pred结果已处理完毕--输出吧");
                            for(WaitTask wwtask:llist){
                                curactor.output(wwtask);
                            }
                            list.remove(llist);   //删除这个llist
                        }else{//还未处理返回结果
                            System.out.print("T1-8谓词/path还没返回结果||返回结果还未处理,等啊等。。。");
                            do{
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while(curactor.getMessageCount() == 0);

                            System.out.println("T1-8谓词/path返回结果了--先处理 predR/pathR，当前结束标签重新入消息队列");
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage,curactor,curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-8.path/pred 匹配失败！");
                        list.remove(llist);   //删除这个为空的llist
                    }
                }else{ //肯定是要上传的，但是若此时还未处理path的返回结果，就该等待先处理--最后一个llist中的元素
                    System.out.print("T1-8是后续path，");
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            //需要当前方法return，去处理下一个消息--即谓词返回结果，并保存当前（index，id）
                            System.out.println("T1-8的path/pred还未返回结果，等待path/pred的结果处理了再继续扫描！");
                            while(curactor.getMessageCount()==0){
                                try {
                                    Thread.sleep(1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            DefaultMessage message = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(message, curactor, curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-8.path/pred 匹配失败！");
                        list.remove(llist);   //删除这个为空的llist
                    }
                }
            }else{
                System.out.println("T1-8未找到匹配的开始标记");
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签(肯定是作为后续path)
            // (能遇到上层结束标签，即T1-6作为一个后续的path（T1-5 的时候也会放在stackActor中），T1-6~T1-8会被放在paActor中)
            // T1-5 的后续的path时，与T1-5 放在同一个栈，T1-6~T1-8 放在pathstack中
            System.out.println("T1-8遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();
            int idd = task.getId();

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
                    curactor.sendPathResult(new ActorTask(idd,new Object[]{num,wtask},isInself));
                    if(!ss.isEmpty()){
                        task = (ActorTask)(ss.peek());
                        State currstate = (State)task.getObject();
                        if(currstate instanceof StateT1_5){
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage, curactor, curactor);
                            return false;
                        }else if(currstate instanceof StateT1_8){
                            //T1-7作为AD轴test的后续path，即T1-7/T1-8
                            curactor.processSameADPath(new Object[]{task.getId(),num,wtask});
                        }
                    }
                }else {
                    System.out.println("T1-8 path/pred检查失败，上传 NF");
                    curactor.sendPathResult(new ActorTask(idd, new Object[]{0, "NF"}, isInself));
                }
            }else{
                System.out.println("T1-8没遇到其开始标记&&遇到了上层结束标记，上传 NF");
                curactor.sendPathResult(new ActorTask(idd, new Object[]{0, "NF"}, isInself));
            }

            if(!ss.isEmpty()){
                State currstate = (State)((ActorTask)ss.peek()).getObject();
                if(currstate instanceof StateT1_5){
                    dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                    actorManager.send(dmessage, curactor, curactor);
                    return false;
                }
            }
        }
        return true;
    }

    /*
   * 收到谓词的返回结果：
   * 找到T1-8.list的中最后一个llist，设置
   * */
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {
        Boolean pred = (Boolean)atask.getObject();
        System.out.print("T1-8 处理 predR，");
        for(int i = list.size()-1;i >= 0;i--){
            ArrayList<WaitTask> llist = (ArrayList<WaitTask>)list.get(i);
            if(!llist.isEmpty()){
                WaitTask wt = llist.get(0);
                if(wt.getId() == atask.getId()){
                    if(pred){
                        System.out.println("preR==true，设置 llist 中所有 wt ");
                        for(WaitTask wtt:llist)
                            wtt.setPredR(pred);//true--设置
                    } else {
                        System.out.println("preR==false，清空llist");
                        llist.clear();  //false--清空当前llist
                        //告诉path不用继续检查了--
                    }
                    return;
                }
            }
        }
    }

    /*处理返回的path结果
   *   atask={num,tag}
   *   此时T1-8的最后一个等待list中只有一个wt，则需要copy num-1 份
   *
   * */
    @Override
    public void pathMatchFunction(ActorTask atask) {
        System.out.print("T1-8 处理pathR，");
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
            //else--已经是空的（即pred返回false），若输出--删除空的llist、、若上传，计算的时候size == 0
        }
    }
}