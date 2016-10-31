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
            linkList.addNode((ActorTask[]) message.getData());
            if(linkList.getSize() == 1){    //第一次发送
                DefaultMessage messages = new DefaultMessage("nodeID",new Object[]{++index,0});
                System.out.println(manager.getActors()[0].getName());
                this.getManager().send(messages,this,manager.getActors()[0]);  //mainActor
            }
        }else if(subject.equals("modifyIndex")) {     //要求修改next的时候
            int index = (Integer) message.getData();
            if (++index < linkList.getSize()) {
                DefaultMessage messages = new DefaultMessage("nodeID", new Object[]{index, 0});
                manager.send(messages, this, message.getSource());
            } else {  //输入没了
                DefaultMessage messages = new DefaultMessage("wait", new Object[]{--index, 1000});
                manager.send(messages, this, message.getSource());  // 等 1s
            }
        }
    }

    public LinkList getLinkList() {
        return linkList;
    }
}
