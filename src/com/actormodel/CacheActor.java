package com.actormodel;

import com.ibm.actor.AbstractActor;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;
import com.taskmodel.ActorTask;
import com.taskmodel.LinkList;

/**
 * Created by qin on 2016/10/3.
 */
public class CacheActor extends AbstractActor {
    public static LinkList linkList = new LinkList();//链表
    protected int index = 0;//数据块的引用
    public CacheActor(){}
    //发出的消息--输入用完、数据块的引用
    //收到的消息--SAX 那里来的数据块（数组）、actor需要修改数据块的引用
    @Override
    protected void loopBody(Message message) {
        sleep(1);
        String subject = message.getSubject();

        if(subject.equals("node")){  //收到数据块
//            System.out.println(this.getName() + " 中添加第" + ++index + "块数据块");
            ++index;
            linkList.addNode((ActorTask[]) (((ActorTask) message.getData()).getObject()));
            if(linkList.getSize() == 1){    //第一次发送
                System.out.println(this.getName() + " 第一次发送数据块给 mainActor");
                DefaultMessage messages = new DefaultMessage("nodeID",new Object[]{index,0});
                this.getManager().send(messages,this,manager.getActors()[0]);  //mainActor
            }
        }else if(subject.equals("modifyIndex")) {     //要求修改next的时候
            int index = (Integer) message.getData();  //当前的处理完的数据块的index
            if (index <= linkList.getSize()) {
                System.out.println(this.getName() + "被 "+message.getSource().getName()+" 要求修改数据块的 index && 能够修改index");
                DefaultMessage messages = new DefaultMessage("nodeID", new Object[]{index, 0});
                manager.send(messages, this, message.getSource());
            } else {  //输入没了
                System.out.println(this.getName() + "被 "+message.getSource().getName()+" 要求修改数据块的 index && 修改index 失败，告诉要求取数据的 actor 先等待");
                DefaultMessage messages = new DefaultMessage("wait", new Object[]{index, 10});
                manager.send(messages, this, message.getSource());  // 等 10ms
            }
        }
    }

    public LinkList getLinkList() {
        return linkList;
    }
}
