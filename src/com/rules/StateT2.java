package com.rules;

import com.XPath.PathParser.ASTPreds;
import com.XPath.PathParser.AxisType;
import com.actormodel.TaskActor;
import com.taskmodel.ActorTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT2 extends State implements Cloneable{

    protected ASTPreds _preds;
    protected String _test;

    protected Stack _predstack;

    protected StateT2(ASTPreds preds){
        _preds = preds;
        _test  = _preds.getFirstStep().getNodeTest().toString();

    }

    public static StateT2 TranslateStateT2(ASTPreds preds){
        //根据轴类型选择性的调用T2规则
        if(preds.getFirstStep().getAxisType()== AxisType.PC)
        {
            if (preds.getFirstStep().getPreds().toString().equals(""))
                return StateT2_1.TranslateState(preds);//无后续谓词
            return StateT2_2.TranslateState(preds);//有后续谓词
        }else{
            if (preds.getFirstStep().getPreds().toString().equals("")) //AD
                return StateT2_3.TranslateState(preds);//无后续谓词
            return StateT2_4.TranslateState(preds);//有后续谓词
        }
    }


    public State copy() throws CloneNotSupportedException {
        return (State)this.clone();
    }

    @Override
    public boolean startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {
        return true;
    }

    @Override
    public boolean endElementDo(int index,int id,ActorTask atask,TaskActor curactor){
        return true;
    }

    @Override
    public void pathMatchFunction(ActorTask atask) {

    }
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {

    }


}
