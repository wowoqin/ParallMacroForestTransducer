package com.rules;

import com.XPath.PathParser.ASTPreds;
import com.XPath.PathParser.AxisType;
import com.actormodel.TaskActor;
import com.taskmodel.ActorTask;

import java.util.Stack;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT3 extends State implements Cloneable{

    protected  ASTPreds _preds;
    protected  String   _test;
    protected  Stack    _predstack;


    protected StateT3(ASTPreds preds){
        _preds = preds;
        _test  = _preds.getFirstStep().getNodeTest().toString();
        _predstack = new Stack();
    }

    //得到当前谓词的的第一个谓词，也就是【child::test preds】或者【desc_or_self::test preds】
    public static ASTPreds getSinglePred(ASTPreds preds){
        ASTPreds single = new ASTPreds();
        single.setFirstStep(preds.getFirstStep());
        single.setRemainderPreds(ASTPreds.nil);
        return single;
    }

    public static ASTPreds getRemainPred(ASTPreds preds){
        return preds.getRemainderPreds();
    }

    //根据轴类型选择性的调用T3规则
    public static State TranslateStateT3(ASTPreds preds){
        if(preds.getRemainderPreds().toString().equals("")){ //T2
            return StateT2.TranslateStateT2(preds);
        }else{
            if(preds.getFirstStep().getAxisType()== AxisType.PC) {  //PC
                if (preds.getFirstStep().getPreds().toString().equals(""))
                    return StateT3_1.TranslateState(preds);
                else return StateT3_2.TranslateState(preds);
            }else{  //AD
                if (preds.getFirstStep().getPreds().toString().equals(""))
                    return StateT3_3.TranslateState(preds);
                else return StateT3_4.TranslateState(preds);
            }
        }
    }

    public State get_q2() {
        return null;
    }

    public State get_q3() {
        return null;
    }

    public Stack get_predstack() {
        return _predstack;
    }

    public State copy() throws CloneNotSupportedException {
        return (State)this.clone();
    }

    @Override
    public void startElementDo(int index,int id,ActorTask atask,TaskActor curactor) throws CloneNotSupportedException {}

    @Override
    public void endElementDo(int index,int id,ActorTask atask,TaskActor curactor){}

    @Override
    public void pathMatchFunction(ActorTask atask) {

    }
    @Override
    public void predMatchFunction(ActorTask atask,TaskActor curractor) {

    }

}
