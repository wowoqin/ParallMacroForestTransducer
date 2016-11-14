package com.xml.sax;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

/**
 * Created by qin on 15-4-29.
 */
public class SaxTest {
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        System.out.println(Thread.currentThread().getName()+" 线程开始运行");
//        Thread.currentThread().setPriority(1);
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        //File f = new File("test3.xml");
        //MySaxParser dh = new MySaxParser("/a//d[//c[/d]][/a]");
        //MySaxParser dh = new MySaxParser("/a/c[/b][/d]");
        //MySaxParser dh = new MySaxParser("/a/c/d[/a]");
        File f = new File("test8.xml");
        MySaxParser dh = new MySaxParser("//a/b");
        //MySaxParser dh = new MySaxParser("//a[/b]//d");
        //MySaxParser dh = new MySaxParser("/a[/b[/c]]/e[/f]");
        //MySaxParser dh = new MySaxParser("//a[/d]/c[/b[//g]]");
        parser.parse(f, dh);

        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName() + " 线程结束运行");
    }
}
