package com.XPath.PathParser;
public class NodeTest {
	private String tag;
	
	public NodeTest(){
		
	}
	
	public NodeTest(String tagp){
		tag = tagp;
	}
	
	public String getTag(){
		return this.tag;
	}
	
	public void setTag(String tag){
		this.tag = tag;
	}
	
	public String toString(){
		return tag;
	}
}
