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
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
//        File f = new File("test8.xml");
//        MySaxParser dh = new MySaxParser("//a[//c]/d/e");

        File f = new File("C:\\Users\\qin\\Desktop\\test\\sample1.xml");
//        MySaxParser dh = new MySaxParser("//item/description/parlist");
        MySaxParser dh = new MySaxParser("//name");
        parser.parse(f, dh);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(Thread.currentThread().getName() + " 线程结束运行");
    }
}
