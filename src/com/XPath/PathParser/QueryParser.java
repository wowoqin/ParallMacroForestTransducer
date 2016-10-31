package com.XPath.PathParser;
public class QueryParser {

	public QueryParser() {

	}
	
	public ASTPath parseXPath(String path) {
		StringBuffer pathBuffer = new StringBuffer(path);
		return parseXPath(pathBuffer);
	}
	
	private ASTPath parseXPath(StringBuffer pathBuffer) {
		if (null == pathBuffer || pathBuffer.length() == 0) {
			return ASTPath.nil;
		}
		
		ASTPath astPath = new ASTPath();
		astPath.setFirstStep(stringToStep(getFirstStepString(pathBuffer)));
		astPath.setRemainderPath(parseXPath(pathBuffer));		
		return astPath;
	}
	
	private String getFirstStepString(StringBuffer pathBuffer){
		if (null == pathBuffer || pathBuffer.length() == 0) {
			return null;
		}
		
		int length = pathBuffer.length();
		int depth = 0;
		int end= 0;
		if (pathBuffer.charAt(0) == '.') {
			if (pathBuffer.charAt(1) == '/') {
				if (pathBuffer.charAt(2) == '/') {
					if (pathBuffer.charAt(3) == '/') {
						assert false;
					}
					end = 3;
				}
				else {
					end = 2;
				}
			}
			else {
				assert false;
			}
		}
		else if (pathBuffer.charAt(0) == '/') {
			if (pathBuffer.charAt(1) == '/') {
				end = 2;
			}
			else {
				end = 1;
			}
		}
		
		while (end < length) {
			boolean endFlag = false;
			switch (pathBuffer.charAt(end)) {
			case '[':
				depth++;				
				break;
			case ']':
				depth--;
				break;
			case '/':
				if (depth == 0) {
					endFlag = true;
				}
				break;
			default:
				break;
			}
			
			if (endFlag) {
				break;
			}
			end++;			
		}
				
		String rtn = pathBuffer.substring(0, end);
		pathBuffer.delete(0, end);
		return rtn;
	}
	
	private ASTStep stringToStep(String stepString){
		if (stepString == null || stepString.length() == 0) {
			return null;
		}
		
		ASTStep step = new ASTStep();
		int length = stepString.length();
		int begin = 0, end= 0;
		if (stepString.charAt(0) == '.') {
			if (stepString.charAt(1) == '/') {
				if (stepString.charAt(2) == '/') {
					if (stepString.charAt(3) == '/') {
						assert false;
					}
					
					begin = 3;
					step.setAxisType(AxisType.AD);
				}
				else {
					begin = 2;
					step.setAxisType(AxisType.PC);
				}
			}
			else {
				assert false;
			}
		}
		else if (stepString.charAt(0) == '/') {
			if (stepString.charAt(1) == '/') {
				begin = 2;
				step.setAxisType(AxisType.AD);
			}
			else {
				begin = 1;
				step.setAxisType(AxisType.PC);
			}
		}
		else {
			step.setAxisType(AxisType.PC);
		}
		
		end = begin;
		while(end < length){
			char tempChar = stepString.charAt(end);   
			if (tempChar == '[' || tempChar == '/') {
				break;
			}
			end++;
		}
		step.setNodeTest(new NodeTest(stepString.substring(begin, end)));

		if (end == length) {
			step.setPreds(ASTPreds.nil);
			return step;
		}
		
		StringBuffer predsBuffer = new StringBuffer(stepString.substring(end, length));
		if (stepString.charAt(end) == '/') {
			predsBuffer.insert(0, "[");
			predsBuffer.append("]");
		}
		step.setPreds(stringToPreds(predsBuffer));		
		return step;
	}
	
	private ASTPreds stringToPreds(StringBuffer predsBuffer) {
		if (predsBuffer == null || predsBuffer.length() == 0) {
			return ASTPreds.nil;
		}

		ASTPreds preds = new ASTPreds();
		String firstPredString = getFirstPredString(predsBuffer);
		preds.setFirstStep(stringToStep(firstPredString));
		preds.setRemainderPreds(stringToPreds(predsBuffer));
		return preds;
	}
	
	private String getFirstPredString(StringBuffer predsBuffer) {
		if (predsBuffer == null || predsBuffer.length() == 0) {
			return null;
		}
		
		int length = predsBuffer.length();
		int depth = 0;
		int end = 1;
		if (predsBuffer.charAt(0) == '[') {
			while (end < length) {
				boolean endFlag = false;
				switch (predsBuffer.charAt(end)) {
				case '[':
					depth++;				
					break;
				case ']':
					if (depth == 0) {
						endFlag = true;
					}
					else {
						depth--;
					}
					break;
				default:
					break;
				}
				
				if (endFlag) {
					break;
				}
				end++;
			}
			
			if (end == length) {
				assert false;
			}
		}
		else {
			assert false;
		}
		
		String rtn = predsBuffer.substring(1, end);
		predsBuffer.delete(0, end + 1);
		return rtn;
	}
}
