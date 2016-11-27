//package com.actormodel;
//
//import com.ibm.actor.*;
//
//import java.util.Date;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
///**
// * Created by qin on 2016/11/26.
// */
//public class ActorManager extends DefaultActorManager {
//    public int send(Message message, Actor from, Actor to) {
//        int count = 0;
//        if(message != null) {
//            AbstractActor aa = (AbstractActor)to;
//            if(aa != null && !aa.isShutdown() && !aa.isSuspended() && aa.willReceive(message.getSubject())) {
//                DefaultMessage xmessage = (DefaultMessage)((DefaultMessage)message).assignSender(from);
//                aa.addMessage(xmessage);
//                xmessage.fireMessageListeners(new MessageEvent(aa, xmessage, MessageEvent.MessageStatus.SENT));
//                ++this.sendCount;
//                this.lastSendTime = (new Date()).getTime();
//                Map var7;
//                if(this.recordSentMessages) {
//                    var7 = this.sentMessages;
//                    synchronized(this.sentMessages) {
//                        String aname = aa.getName();
//                        Object l = (List)this.sentMessages.get(aname);
//                        if(l == null) {
//                            l = new LinkedList();
//                            this.sentMessages.put(aname, l);
//                        }
//
//                        if(((List)l).size() < 100) {
//                            ((List)l).add(xmessage);
//                        }
//                    }
//                }
//
//                ++count;
//                var7 = this.actors;
//                synchronized(this.actors) {
//                    this.actors.notifyAll();
//                }
//            }
//        }
//
//        return count;
//    }
//}
