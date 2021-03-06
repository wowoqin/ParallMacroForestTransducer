package com.rules;

import com.XPath.PathParser.ASTPreds;

/**
 * Created by qin on 2015/10/10.
 */
public class StateT3_3 extends StateT3{
    protected  State _q2;//检查【desc_or_self::test】
    protected  State _q3;//检查 preds'

    protected  StateT3_3(ASTPreds preds,State q2,State q3){
        super(preds);
        _q2 = q2;
        _q3 = q3;
    }

    public static StateT3 TranslateState(ASTPreds preds){//重新创建T3-3
        State q2 = StateT2.TranslateStateT2(StateT3.getSinglePred(preds));
        State q3 = StateT3.TranslateStateT3(preds.getRemainderPreds());
        return new StateT3_3(preds,q2,q3);//然后压入栈
    }
    @Override
    public State get_q2() {
        return _q2;
    }
    @Override
    public State get_q3() {
        return _q3;
    }
}

