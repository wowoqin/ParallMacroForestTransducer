package com.taskmodel;

/**
 * Created by qin on 2016/10/3.
 *
 * 每个节点100个标签
 */
public class Node {
    protected ActorTask[] atasks=new ActorTask[100];//
    protected Node next;

    public Node(ActorTask[] tasks){
        this.atasks = tasks;
        this.next = null;
    }
    public ActorTask[] getAtask() {
        return atasks;
    }

    public Node getNext() {
        return next;
    }

    public void setNext(Node next) {
        this.next = next;
    }

    public void setAtask(ActorTask[] atasks) {
        this.atasks = atasks;
    }

}
