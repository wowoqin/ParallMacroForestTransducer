package com.taskmodel;

import java.util.Stack;

/**
 * Created by qin on 2016/3/28.
 *
 */
public class ActorTask {  // actor 之间交互的数据
    /*
    * 返回的检查结果（ActorTask）的 id 与 tlist 中的等待任务模型（WaitTask）的id 相匹配
    * */
    protected Stack pstack;
    protected int id;
    // 发送给 actor的数据,如： q（State）、qName（String）、q'的返回结果（True/False）、q''的返回结果（String）
    protected Object object;
    protected boolean isInSelf;      //标识检查结果传给自己还是传给上级actor
    protected int indexs;
    protected int arrid;

    public ActorTask(int id, Object object, boolean flg) {
        //栈内元素(id,q,isInSelf)、返回消息（id,true/tag,isInself)
        this.id = id;
        this.object = object;
        this.isInSelf = flg;
    }

    public ActorTask(Stack pstack, int id, Object object, boolean isInSelf, int indexs, int arrid) {
        //res&push
        this.pstack = pstack;
        this.id = id;
        this.object = object;
        this.isInSelf = isInSelf;
        this.indexs = indexs;
        this.arrid = arrid;
    }

    public ActorTask(int id, Object object, boolean isInSelf, int indexs, int arrid) {
        // pushFunction
        this.id = id;
        this.object = object;
        this.isInSelf = isInSelf;
        this.indexs = indexs;
        this.arrid = arrid;
    }

    public ActorTask(int indexs, int arrid) {
        this.indexs = indexs;
        this.arrid = arrid;
    }

    public int getId() {
        return id;
    }

    public Object getObject() {
        return object;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setObject(Object[] object) {
        this.object = object;
    }

    public boolean isInSelf() {
        return isInSelf;
    }

    public Stack getPstack() {
        return pstack;
    }

    public int getIndexs() {
        return indexs;
    }

    public int getArrid() {
        return arrid;
    }

    public void setIndexs(int indexs) {
        this.indexs = indexs;
    }

    public void setArrid(int arrid) {
        this.arrid = arrid;
    }
}
