package com.XPath.PathParser;

public class ASTPreds extends ASTExpr {
	public static ASTPreds nil = new ASTPreds();
	private ASTStep firstStep;
	private ASTPreds remainderPreds;
	
	public ASTPreds(){
		
	}
	
	public ASTPreds getRemainderPreds() {
		return this.remainderPreds;
	}
	
	public void setRemainderPreds(ASTPreds remainderPreds) {
		this.remainderPreds = remainderPreds;
	}
	
	public ASTStep getFirstStep(){
		return this.firstStep;		
	}
	
	public void setFirstStep(ASTStep firstStep){
		this.firstStep = firstStep;
	}

	public String toString() {
		if (this == nil) {
			//return "[nil]";
			return "";
		}
		
		String firstStepString = "[" + firstStep.toString() + "]";
		String remainderPredsString = remainderPreds.toString();		
		return firstStepString + remainderPredsString;
	}
	
	public void accept(IASTVisitor visitor){
		visitor.visitPreds(this);
	}
}
