package com.xml.sax;

import com.XPath.PathParser.ASTPath;
import com.XPath.PathParser.QueryParser;
import com.actormodel.CacheActor;
import com.actormodel.TaskActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultMessage;
import com.rules.State;
import com.rules.StateT1;
import com.taskmodel.ActorTask;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Objects;
import java.util.Stack;

/**
 * Created by qin on 15-4-27.
 */
public class MySaxParser<T> extends DefaultHandler {

    protected QueryParser qp ;
    protected ASTPath path;
    protected int layer;

    // SAX 接口处的引用
    protected DefaultActorManager manager= State.actorManager;
    protected DefaultMessage message;
    protected Actor cacheActor;
    protected Actor mainActor;
    protected ActorTask[] array = new ActorTask[2];
    protected int id = 0;    //元素的索引
    protected int index = 0; //数据块的索引

    public MySaxParser(String path_str) {
        super();
        qp = new QueryParser();
        path = qp.parseXPath(path_str);
        State currentQ = StateT1.TranslateStateT1(path);//将XPath翻译为各个状态
        Stack stack = new Stack();

        mainActor = manager.createAndStartActor(TaskActor.class, "mainActor");
        message = new DefaultMessage("res&&push",new Object[]{stack, new ActorTask(currentQ.getLevel(),new Object[]{currentQ},true)});
        manager.send(message, null, mainActor);
    }


    @Override
    public void startDocument() throws SAXException {
//        System.out.println("----------- Start Document ----------");
        cacheActor = manager.createAndStartActor(CacheActor.class,"cacheActor");
        State.actors.put(cacheActor.getName(),cacheActor);
        layer = 0;
        super.startDocument();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if(id == array.length-1){
            array[id] = new ActorTask(layer,qName,true);
            while(cacheActor.getMessageCount() == 90){
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            message = new DefaultMessage("node",new ActorTask(index++,array,true));
            manager.send(message,null,cacheActor);
            array = new ActorTask[array.length];
            id = 0;
        }else{
            array[id++] = new ActorTask(layer,qName,true);
        }
        layer++;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        layer--;
        if(id == array.length-1){
            array[id] = new ActorTask(layer,qName,false);
            while(cacheActor.getMessageCount() == 90){
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            message = new DefaultMessage("node",new ActorTask(index++,array,true));
            manager.send(message,null,cacheActor);
            array = new ActorTask[array.length];
            id = 0;
        }else{
            array[id++] = new ActorTask(layer,qName,false);
        }
    }

    @Override
    public void endDocument() throws SAXException{
        super.endDocument();
//        System.out.println("----------- End  Document ----------");

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        String content = new String(ch,start,length);
        super.characters(ch, start, length);
    }

}