package com.XPath.PathParser;


public class ASTStep extends ASTExpr {
	private AxisType stepType;
	private NodeTest nodeTest;
	private ASTPreds preds;
	
	public ASTStep() {
		
	}
	
	public AxisType getAxisType(){
		return this.stepType;
	}
	
	public void setAxisType(AxisType stepType){
		this.stepType = stepType;
	}
	
	public NodeTest getNodeTest(){
		return this.nodeTest;
	}
	
	public void setNodeTest(NodeTest nodeTest){
		this.nodeTest = nodeTest;
	}
	
	public ASTPreds getPreds() {
		return this.preds;
	}
	
	public void setPreds(ASTPreds preds) {
		this.preds = preds;
	}
	
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		if (stepType == AxisType.PC) {
			buffer.append("/");
		}
		else if (stepType == AxisType.AD) {
			buffer.append("//");
		}
		else {
			assert false;
		}
		
		buffer.append(nodeTest.toString());
		buffer.append(preds.toString());
		return buffer.toString();
	}
	
	public void accept(IASTVisitor visitor) {
		visitor.visitStep(this);
	}
}
