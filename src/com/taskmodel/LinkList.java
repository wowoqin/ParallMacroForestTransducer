package com.taskmodel;

/**
 * Created by qin on 2016/10/3.
 */
public class LinkList {
    protected Node first;
    protected Node last;
    protected int size;

    public LinkList(){
        this.first = this.last = null;
        this.size = 0;
    }

    public Node getFirst() {
        return first;
    }

    public Node getLast() {
        return last;
    }

    public int getSize() {
        return size;
    }

    public void setFirst(Node first) {
        this.first = first;
    }

    public void setLast(Node last) {
        this.last = last;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Node getNode(int index){  //根据索引找到对应的节点
        int count = 0;
        for(Node node = first; node!= null; node = node.next){
            count ++;
            if(count == index)
                return node;
        }
        return null;
    }

    public Node addNode(ActorTask[] atask){   //链表尾部添加新节点
        Node newNode = new Node(atask);
        if(!isEmpty()){
            this.last.next = newNode;
            this.setLast(newNode);
        }else{
            first = last = newNode;
        }
        size++;
        return newNode;
    }

    public boolean isEmpty(){
        return first == null;
    }
}
