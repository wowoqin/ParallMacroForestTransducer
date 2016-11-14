package com.rules;

import com.XPath.PathParser.ASTPath;
import com.actormodel.TaskActor;
import com.ibm.actor.DefaultMessage;
import com.taskmodel.ActorTask;
import com.taskmodel.WaitTask;

import java.util.LinkedList;
import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT1_5 extends StateT1{
    protected  State  _q1;//检查 后续 path

    protected StateT1_5(ASTPath path,State q1){
        super(path);
        _q1=q1;
    }

    public static State TranslateState(ASTPath path){//重新创建T1-5
        State q1=StateT1.TranslateStateT1(path.getRemainderPath());
        return new StateT1_5(path,q1);
    }

    /*
    * T1-5--每一个T1-5都有多个匹配结果，所以每一个T1-5的开始标签对应一个list
    *   -- 输出：list中只有一个list，多个元素 -- {{wt1,wt2,wt3}}
    *   -- 上传：list中多个list，多个元素 -- {{wt1,wt2,wt3},{wt1,wt2,wt3},{wt1,wt2,wt3}}
    *   -- 返回给T1-5的结果的匹配都是相对于其最后一个list而言 && 结果返回了之后就检查淘汰--上传的时候就不用检查了
    *  */

//    @Override
//    public void addWTask(WaitTask wtask){
//        this.list.add(new LinkedList<WaitTask>().add(wtask));
//    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException{
        int layer = atask.getId();
        String tag = atask.getObject().toString();

        if((getLevel() == layer) && (tag.equals(_test))) {
            System.out.println("T1-5匹配到开始标签，q1压栈 && add(wt)");
            list.add(new LinkedList<WaitTask>());
            addWTask(new WaitTask(layer, true, null));
            _q1.setLevel(layer + 1);
            curactor.pushTaskDo(new ActorTask(layer,_q1,true));
        }
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        int layer = atask.getId();
        String tag = atask.getObject().toString();
        // T1-5能遇到自己的结束标签，则T1-5.q1 已经弹栈了，而T1-5.q1 弹栈则说明 q1 已经检查完 && 传回了结果，
        // 所以不可能在遇到结束标签的时候还未处理返回结果
        if (getLevel() == layer && tag.equals(_test)) { //遇到自己的结束标签，若是要输出，则只有一个list
            if(!list.isEmpty()){
                if(curactor.getName().equals("mainActor") && (curactor.getMyStack().size()==1)){
                    System.out.print("T1-5 是个 XPath，");
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(0);
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(wtask.hasReturned()){
                            System.out.println("T1-5的 path 结果已处理完毕--输出llist");
                            for(WaitTask wwtask:llist)
                                curactor.output(wwtask);
                        } else{  //还未处理返回结果
                            System.out.println("T1-5的path结果还未处理--先处理pathR，重新将结束标签发送给自己");
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage, curactor, curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-5未找到匹配的开始标记 || path返回结果为NF");
                        list.remove(llist);   //删除最后一个为空的llist
                    }
                }else{
                    System.out.println("T1-5是个后续path--检查最后一个list的path的返回情况");
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size()-1);
                    if(!llist.isEmpty()){
                        WaitTask wtask = llist.get(0);
                        if(!wtask.hasReturned()){
                            dmessage = new DefaultMessage("nodeID",new Object[]{index,id});
                            actorManager.send(dmessage, curactor, curactor);
                            return false; //中断此次处理--先处理返回的结果
                        }
                    }else{
                        System.out.println("T1-5未找到匹配的开始标记 || path返回结果为NF");
                        list.remove(llist);   //删除最后一个为空的llist
                    }
                }
            }else{
                System.out.println("T1-5没有遇到自己的开始标记");
            }
        }else if (layer == getLevel() - 1) { // 遇到上层结束标签
            // (能遇到上层结束标签，即T1-5作为一个后续的path（T1-5 的时候也会放在stackActor中），T1-6~T1-8会被放在paActor中)
            // 传递整个list中的所有list的size之和个个数
            System.out.println("T1-5遇到上层结束标签-->传递结果");
            Stack ss = curactor.getMyStack();
            ActorTask task = (ActorTask)ss.peek();
            boolean isInself = task.isInSelf();

            if(!list.isEmpty()){
                int num = 0;
                WaitTask wtask = null;

                for(int i=0;i<list.size();i++){
                    LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(i);
                    if(!llist.isEmpty()){
                        num += llist.size();   //上传的数量
                        if(wtask == null)
                            wtask = llist.get(0);
                    }
                }

                if(num > 0){
                    curactor.sendPathResult(new ActorTask(0, new Object[]{num, wtask}, isInself));
                } else{
                    curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
                }

            }else{
                System.out.println("T1-5无上传结果--传递NF");
                curactor.sendPathResult(new ActorTask(0, new Object[]{0, "NF"}, isInself));
            }

            //返回结果之后pop（T1-5），看当前栈顶
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
    /*  处理返回的path结果
    *      atask = {num,tag}
    *      此时T1-5的最后一个等待list中只有一个wt(id,true,null)，则需要copy(num-1) 份
    *
    * */
    @Override
    public void pathMatchFunction(ActorTask atask) {
        System.out.print("T1-5 处理pathR，");
        LinkedList<WaitTask> llist = (LinkedList<WaitTask>)list.get(list.size() - 1);  //最后一个list
        Object[] obj = (Object[])atask.getObject();
        int num = (Integer)obj[0];
        if(num == 0){
            System.out.println("pathR==notFound，清空llist");
            llist.clear();     //清空llist
        } else {
            System.out.println("返回了 "+num+" 个pathR，对llist进行设置");
            String tag = (String) obj[1];
            WaitTask wt = llist.get(0);
            wt.setPathR(tag);
            for(int i = 1;i < num;i++)
                llist.add(wt);
        }
    }
}
