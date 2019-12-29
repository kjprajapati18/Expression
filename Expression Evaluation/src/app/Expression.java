package app;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import structures.Stack;

public class Expression {

	public static String delims = " \t*+-/()[]";
			
    /**
     * Populates the vars list with simple variables, and arrays lists with arrays
     * in the expression. For every variable (simple or array), a SINGLE instance is created 
     * and stored, even if it appears more than once in the expression.
     * At this time, values for all variables and all array items are set to
     * zero - they will be loaded from a file in the loadVariableValues method.
     * 
     * @param expr The expression
     * @param vars The variables array list - already created by the caller
     * @param arrays The arrays array list - already created by the caller
     */
	private static boolean isNumeric(char x) {
		if(x >= 48 && x<=57) return true;
		return false;
	}
	
    public static void 
    makeVariableLists(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	/** DO NOT create new vars and arrays - they are already created before being sent in
    	 ** to this method - you just need to fill them in.
    	 **/
    	for(int i=0; i<expr.length(); i++) {
    		char x = expr.charAt(i);
    		if(delims.indexOf(x) != -1 || isNumeric(x)) {
    			//if its an operator or space or number, then move on
    			continue;
    		}
    		
    		//At this point in the code, it can only be a letter
    		int endIndex = i+1;
    		while(endIndex < expr.length()
    				&& delims.indexOf(expr.charAt(endIndex)) == -1
    				&& !isNumeric(expr.charAt(endIndex))) {
    			endIndex++;
    		}
    		
    		String name = expr.substring(i,endIndex);
    		
    		if(endIndex >= expr.length()-1) { //Prevents OutOfBounds.
    			vars.add(new Variable(name));
    			continue;
    		}
    		
    		if(expr.charAt(endIndex) == '[') {	//We found an array
    			
    			Array temp = new Array(name);
    			if(arrays.indexOf(temp) == -1) { //Brand new array
    				arrays.add(temp);
    			}
    			
    		} else {						//We found a variable
    			Variable temp = new Variable(name);
    			if(vars.indexOf(temp) == -1) {//New Variable
    				vars.add(temp);
    			}
    		}
    		
    		i = endIndex;
    		
    	}
    }
    
    /**
     * Loads values for variables and arrays in the expression
     * 
     * @param sc Scanner for values input
     * @throws IOException If there is a problem with the input 
     * @param vars The variables array list, previously populated by makeVariableLists
     * @param arrays The arrays array list - previously populated by makeVariableLists
     */
    public static void 
    loadVariableValues(Scanner sc, ArrayList<Variable> vars, ArrayList<Array> arrays) 
    throws IOException {
        while (sc.hasNextLine()) {
            StringTokenizer st = new StringTokenizer(sc.nextLine().trim());
            int numTokens = st.countTokens();
            String tok = st.nextToken();
            Variable var = new Variable(tok);
            Array arr = new Array(tok);
            int vari = vars.indexOf(var);
            int arri = arrays.indexOf(arr);
            if (vari == -1 && arri == -1) {
            	continue;
            }
            int num = Integer.parseInt(st.nextToken());
            if (numTokens == 2) { // scalar symbol
                vars.get(vari).value = num;
            } else { // array symbol
            	arr = arrays.get(arri);
            	arr.values = new int[num];
                // following are (index,val) pairs
                while (st.hasMoreTokens()) {
                    tok = st.nextToken();
                    StringTokenizer stt = new StringTokenizer(tok," (,)");
                    int index = Integer.parseInt(stt.nextToken());
                    int val = Integer.parseInt(stt.nextToken());
                    arr.values[index] = val;              
                }
            }
        }
    }
    
    /**
     * Evaluates the expression.
     * 
     * @param vars The variables array list, with values for all variables in the expression
     * @param arrays The arrays array list, with values for all array items
     * @return Result of evaluation
     */
    public static float 
    evaluate(String expr, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	/** COMPLETE THIS METHOD **/
    	// following line just a placeholder for compilation
    	
    	//Does not work if there is a really small value somewhere
    	//((19 / 45 / (3 - 35) / (((76 / 4 + 75) - (66 - 21 * 13)) - 39 / 57 - 38 - 92 - 29 / 25) + 37 + (33 - ((87 / 97) / 85 / 32) + 39) - 44 - (44 * 89)) + 67 / 63 - (4 + 97 / 1 / (42) / 24 + 92 + 81 * 53 / 14 + 66 - 21 * 88) - 24 / ((35 - 21) - 24) / 33 / 20 + (37 * 84 + 45) / 25 - 83) / (71 + (16 + 84)) + ((70 * 90 * (73) * 88 + 6 / 66 - (24 + 30 * 29) / 68 + 42 * (53 * (89 - 74) / 70 - 51) + 12 - 35 + 93 + 33 / ((59 * 25 / 6) / 18)) * (88 + 33 * ((26 * 91 * 90 - 65 * 61) / 4 - 22)))
    	//Code will break if the numbers are too big or too small. This is because an "E" gets shoved in there (scientific notation)
    	
    	Stack<Integer> brack = new Stack<Integer>();
    	Stack<Integer> paren = new Stack<Integer>();
    	
    	for(int i = 0; i< expr.length(); i++) {
    		char x = expr.charAt(i);
    		
    		if(x == '(') {
    			paren.push(i);
    		} else if(x == '[') {
    			brack.push(i);
    		} else if (x == ')') {
    			
    			int init = paren.pop();
    			float value = evalVar(expr.substring(init+1, i), vars, arrays);
    			expr = expr.substring(0, init) + value + expr.substring(i+1, expr.length());
    			i = init;
    			
    		} else if (x ==']') {
    			int init = brack.pop();
    			int beginArr = prevWordInd(expr, init-1);
    			float value = evalArr(expr.substring(beginArr, i+1), vars, arrays);
    			expr = expr.substring(0, beginArr) + value + expr.substring(i+1, expr.length());
    			i = beginArr;
    		}
    	}
    	//All variables and arrays should have been evaluated and simplified. Parse one more time
    	
    	return evalVar(expr, vars, arrays);
    }
    

    private static String nextWord(String substring, int index) {
    	int temp = index;
    	while(temp != substring.length() && delims.indexOf(substring.charAt(temp)) == -1) {
    		temp++;
    	}
    	return substring.substring(index, temp);
    }
    private static String nextNum(String substring, int index) {
    	int temp = index;
    	if(substring.charAt(temp) == '-' && isNumeric(substring.charAt(temp+1))) {
    		temp++;
    	}
    	while(temp != substring.length() && delims.indexOf(substring.charAt(temp)) == -1) {
    		temp++;
    	}
    	return substring.substring(index, temp);
    }
    private static int prevWordInd(String substring, int index) {
    	int temp = index;
    	while(temp != 0 && delims.indexOf(substring.charAt(temp)) == -1) {
    		temp--;
    	}
    	if(temp ==0) return temp;
    	return temp+1;
    }
    
    
    private static float evalArr(String substring, ArrayList<Variable> vars, ArrayList<Array> arrays) {//Starting from first beginning of array name
    	Stack<Integer> bounds = new Stack<Integer>();
    	String arrName = nextWord(substring,0);
    	
    	for(int i = arrName.length(); i < substring.length(); i++) {
    		char x = substring.charAt(i);
    		if(x == '[') {
    			bounds.push(i);
    		} else if (x == ']') {
    			int index = bounds.pop();
    			int prevInd = prevWordInd(substring, index-1); //"In case there is an array inside the array"
    			if(i == substring.length()-1) {
    				int arrInd = (int) evalVar(substring.substring(index+1, i), vars, arrays);
    				return arrays.get(arrays.indexOf(new Array(arrName))).values[arrInd];
    			}
    			substring = substring.substring(0, prevInd) + 
    					evalArr(substring.substring(prevInd, index),vars,arrays) +
    					substring.substring(i+1, substring.length());
    			i = prevInd;
    		}
    	}
    	return -9999;
    }
    
    public static float evalVar(String substring, ArrayList<Variable> vars, ArrayList<Array> arrays) {
    	//Helper method that evaluates nums and vars. Any expression that does not contain parentheses or brackets
    	String tempWord = "", newSub = "";
    	
    	//Create a new String with all of the variables replaced with values
    	for(int i = 0; i < substring.length(); i++) {
    		char x = substring.charAt(i);
    		if(x == ' ' || x == '\t') continue;
    		if(isNumeric(x) || delims.indexOf(x) != -1 || x == '.'){
    			newSub = newSub + x;
    		} else {
    			tempWord = nextWord(substring, i);
    			newSub = newSub + vars.get(vars.indexOf(new Variable(tempWord))).value;
    			i += tempWord.length()-1;
    		}
    		
    	}
    	
    	//The string now only consists of integers and operators
    	String addSub = "";
    	String prevWord = nextNum(newSub, 0), nextWord;
    	for(int j = prevWord.length(); j < newSub.length(); j++) {
    		char x = newSub.charAt(j);
    		switch(x) {
    			case '*':
    				nextWord = nextNum(newSub, j+1);
    				prevWord = Float.toString(Float.parseFloat(prevWord) * Float.parseFloat(nextWord));
    				j+= nextWord.length();
    				break;
    			case '/':
      				nextWord = nextNum(newSub, j+1);
    				prevWord = String.valueOf(Float.parseFloat(prevWord) / Float.parseFloat(nextWord));
    				j += nextWord.length();
    				break;
    			case '+':
    			case '-':
    				addSub = addSub + prevWord + x;
    				if(j < newSub.length()-1 && newSub.charAt(j+1) == '-') {
    					prevWord = nextNum(newSub, j+1);
    					j += prevWord.length();
    				}
    				break;
    			default:
    					prevWord = nextWord(newSub, j);
    					j += prevWord.length()-1;
    				break;
    		}	
    	}
    	//Now the String consists of only integers, addition, and subtraction
    	addSub = addSub + prevWord;
    	tempWord = nextNum(addSub,0);
    	float sum = Float.parseFloat(tempWord);
    	for(int k = tempWord.length(); k < addSub.length(); k++) {
    		char x = addSub.charAt(k);
    		switch(x) {
    		case '+':
    			tempWord = nextNum(addSub,k+1);
    			sum += Float.parseFloat(tempWord);
    			k += tempWord.length();
    			break;
    		case '-':
    			tempWord = nextNum(addSub,k+1);
    			sum -= Float.parseFloat(tempWord);
    			k += tempWord.length();
    			break;
    		default:
    			break;
    		}
    	}
    	return sum;
    }
}
