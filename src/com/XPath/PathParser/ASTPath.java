package com.XPath.PathParser;

public class ASTPath extends ASTExpr{
	public static ASTPath nil = new ASTPath();
	private ASTStep firstStep;
	private ASTPath remainderPath;
	
	public ASTPath(){
		
	}
	
	public ASTPath getRemainderPath() {
		return remainderPath;
	}
	
	public void setRemainderPath(ASTPath remainderPath) {
		this.remainderPath = remainderPath;
	}
	
	public ASTStep getFirstStep() {
		return firstStep;
	}
	
	public void setFirstStep(ASTStep firstStep) {
		this.firstStep = firstStep;
	}

	public String toString() {
		if (this == nil) {
			//return "Path: nil";
			return "";
		}
		
		String stepString = firstStep.toString();
		String remainderPathString = remainderPath.toString();
		return stepString + remainderPathString;
	}
	
	public void accept(IASTVisitor visitor){
		visitor.visitPath(this);
	}
}
