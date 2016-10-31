package com.taskmodel;

/**
 * Created by qin on 2016/3/28.
 */
public class WaitTask {   // 在 actor 的list 中添加的任务
    protected  int id;          // id
    protected  Boolean predR;   // 谓词的返回结果--TRUE/FALSE
    protected  Object  pathR;   // 后续 path 的返回结果--TAG/NOTFOUND
                                //      preds 的返回结果--"TRUE/FALSE"
    /*
    * 在此：T1-1 ~ T1-4 : pathR 中存放第一步的匹配结果 --> test
    *      T1-5 ~ T1-8 : pathR 中存放后续 path 的检查结果
    *      T2-1 ~ T2-4 : pathR 中直接存放 True
    *      T3-1 ~ T3-4 : pathR 中存放 preds'的检查结果（初始化为--"T3"）
    *
    *      返回的检查结果（ActorTask）的 id 与 tlist 中的等待任务模型（WaitTask）的id 相匹配
    * */

    public WaitTask(int id, Boolean predR, Object pathR) {
        this.id = id;
        this.predR = predR;
        this.pathR = pathR;
    }

    public int getId() {
        return id;
    }

    public Boolean getPredR() {
        return predR;
    }

    public Object getPathR() {
        return pathR;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPredR(Boolean predR) {
        this.predR = predR;
    }

    public void setPathR(Object pathR) {
        this.pathR = pathR;
    }

    public boolean isPredRTrue(){ //谓词为true
        return (getPredR()!= null && getPredR());
    }

    public boolean isPathRTrue(){ //谓词为true
        return (getPathR()!= null && (Boolean)getPathR());
    }

    public boolean hasReturned(){
        return (getPredR() != null && getPathR()!= null );
    }

    public boolean isSatisfiedOut() { // 检查当前 waitTask 是不是已经满足输出条件（可以进行输出操作了）
        return (isPredRTrue() && (getPathR() != null && !(getPathR().equals("NF"))));
    }

    public  boolean isPredsSatisified(){ //wt作为一个谓词，检查成功 (id,true,true)
        return isPredRTrue() && isPathRTrue();
    }

    public  boolean isWaitT3ParallPreds(){ //wt作为一个谓词T3，q'''成功，q''还没检查成功 //(id,true,null)
        return (isPredRTrue() && getPathR() == null);
    }

    public void output(){ //输出最终的检查结果
        System.out.println(this.getPathR());
    }
}
