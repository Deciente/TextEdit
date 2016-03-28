package editor;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Arrays;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;


public class TextStorage{
    //The data structure that stores the characters is a simple doubly linked list (DLL). The rendering information of the document
    //is stored in an arraylist where each element in the arraylist corresponds with a certain line. ie the arraylist element at index 1
    //is simply the node that starts line one.
	public ArrayList<Node> lineList; //ArrayList that points to nodes in the DLL indicating where the lines start
	private int N;        // number of elements on list
    private Node start;   //start sentinel
    private Node end;    //end sentiel
    private Node curr;  //Current node ie. where the cursor is
    private double cursorXPos;
    private double cursorYPos;
    private int currLine; //line where the current node is
    private int lastLine; //Last line in the file
    private int windowHeight;
    private int windowWidth;
    private double scrollShift = 0; //amount scrolling shifts the characters
    private ActionStack<Action> undoStack; //stack containing the actions of items to undo
    private ActionStack<Action> redoStack;  //stack containing the actions of items to redo
    private boolean cursorLimbo = false;  //for when the cursor is at the start of the line
    private int fontSize;
    private String fontName;
    private int margin;
    private int marginRight;
    private static final int changeFactor = 4; //factor at which to change the text size
    public boolean debug = false;
    public boolean gapSkip = false;
    public Node startWord;

    public class Node {
        private Text item; 
        private Node next; 
        private Node prev;

        public Text getItem(){
            return this.item;
        }

        public Node getPrev(){
            return this.prev;
        }

        public Node getNext(){
            return this.next;
        }
    }

    public boolean isEmpty() { 
        return N == 0; 
    }

    public void changeFontSize(int direction){
        this.fontSize += direction * changeFactor;
    }
    
    public int size() { 
        return N;
    }

    public int getMargin(){
        return this.margin;
    }

    public int getWindowWidth(){
        return this.windowWidth;
    }

    public Node getFirst(){
        return this.start.next;
    }

    public Node getEnd(){
        return this.end;
    }

    public int redoSize(){
        //size of the redoStack
        return redoStack.size();
    }

    public int undoSize(){
        //size of the undoStack
        return undoStack.size();
    }

    public void setWindowWidth(int x, boolean rerender){
        //set the width of the window
        this.windowWidth = x;
        if (rerender){
            rerender();
        }
    }

    public void setWindowHeight(int x, boolean rerender){
        //set the height of the window
        this.windowHeight = x;
        if (rerender){
            rerender();
        }
    }

    public double getShift(){
        //get the amount of which the file is shifted by scrolling
        return this.scrollShift;
    }

    public void changeCurrLine(int amount){
        //increment the current line by an amount
        currLine += amount;
    }

    public int getLineNum(){
        return this.lineList.size();
    }

    public Node getCurr(){
        return this.curr;
    }

    public int getCurrLine(){
        return this.currLine;
    }

    public int highestIndex(){
        return this.lineList.size() - 1;
    }

    public double getCursorXPos(){
        return this.cursorXPos;
    }

    public double getCursorYPos(){
        return this.cursorYPos;
    }

    public double getStructureHeight(){
        //returns the height of all the text in the file in pixels
        return height(start.item) * (lineList.size());
    }

    public Text getBuffer(){
        return this.start.item;
    }


    public TextStorage(int windowWidth, int windowHeight, int font, String fontName, int margin, int marginRight) {
        fontSize = font;
        this.margin = margin;
        this.marginRight = marginRight;
        this.fontName = fontName;
        start  = new Node();
        end = new Node();
        start.next = end;
        end.prev = start;
        curr = start;
        N = 0;
        lineList = new ArrayList<Node>();
        cursorXPos = 0;
        cursorYPos = 0;
        currLine = 0;
        lastLine = 0;
        this.windowHeight = windowHeight;
        this.windowWidth = windowWidth;
        this.start.item = new Text(cursorXPos, 0, "");
        this.undoStack = new ActionStack<Action>();
        this.redoStack = new ActionStack<Action>();
    }

    public TextStorage(int windowWidth, int windowHeight, int font, String fontName, File file, int margin, int marginRight){
        fontSize = font;
        this.margin = margin;
        this.marginRight = marginRight;
        this.fontName = fontName;
        start  = new Node();
        end = new Node();
        start.next = end;
        end.prev = start;
        curr = start;
        N = 0;
        lineList = new ArrayList<Node>();
        cursorXPos = 0;
        cursorYPos = 0;
        currLine = 0;
        lastLine = 0;
        this.windowHeight = windowHeight;
        this.windowWidth = windowWidth;
        this.start.item = new Text(cursorXPos, 0, "");
        this.undoStack = new ActionStack<Action>();
        this.redoStack = new ActionStack<Action>();
        parseFile(file);
    }

    public void parseFile(File file){
        //parses a textfile into the data structure
        try{
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);
            int intRead = -1;
            while ((intRead = bufferedReader.read()) != -1) {
                // The integer read can be cast to a char, because we're assuming ASCII.
                char charRead = (char) intRead;
                if (charRead == '\r' || charRead == '\n'){
                    if (charRead == '\r'){bufferedReader.read();}
                    addWithoutRendering("\n");
                } else {
                    addWithoutRendering(Character.toString(charRead));
                }
            }
            rerender();
            bufferedReader.close();
        
        } catch (FileNotFoundException fileNotFoundException) {
            System.out.println("File not found! Exception was: " + fileNotFoundException);
        } catch (IOException ioException) {
            System.out.println("Error when copying; exception was: " + ioException);
        }
    }


    public void addWithoutRendering(String chara) {
        //add to the data structure without rerendering so that opening a file can be done in linear time
        Text text = new Text(chara);
        text.setTextOrigin(VPos.TOP);   
        Node x = new Node();
        if (this.isEmpty()){
            x.item = text;
            curr = x;
            curr.next = end;
            curr.prev = start;
            start.next = curr;
            end.prev = curr;
        } else{
            x.item = text;
            x.next = curr.next;
            x.prev = curr;
            curr.next.prev = x;
            curr.next = x;
            curr = curr.next;
        }
        N++;     
    }

    

    public double cursorAboveScreen(){
        //checks if the cursor is above the screen
        if (-scrollShift > changeIndexToYPos(currLine)){
            return changeIndexToYPos(currLine);
        } else {
            return -1;
        }
    }


    public double cursorBelowScreen(){
        //checks if the cursor is below the screen
        double textHeight = getBuffer().getLayoutBounds().getHeight();
        if (changeIndexToYPos(currLine) + textHeight  > -scrollShift + windowHeight){
            return changeIndexToYPos(currLine + 1) - windowHeight;
        } else {
            return -1;
        }
    }

    public void printStream(){
        //prints the data structure for debugging
        Node i = start;
        while (i != end){
            System.out.println(i.item.getText());
            i = i.next;
        }
    }

    public void rerender(){
        //rerenders the data structure
        int line = 0;       
        restructure();
        while(line < lineList.size()){
            Node i = lineList.get(line);
            double w = margin;
            while (stop(i, line)){
                i.item.setX(w);
                w += width(i.item);
                i.item.setY(changeIndexToYPos(line) + scrollShift);
                i = i.next;
            }
            line ++;         
        }
        cursorLimbo = false;
        setCursorPosToCurr();

    }

    public void rerender(double shift){
        //rerenders the file with a shift. used with scroll bar
        int line = 0;
        this.scrollShift += shift;
        restructure();
        while(line < lineList.size()){
            Node i = lineList.get(line);
            double w = margin;

            while (stop(i, line)){
                i.item.setX(w);
                w += width(i.item);
                i.item.setY(changeIndexToYPos(line) + scrollShift);
                i = i.next;
            }
            line ++;
            
        }
        cursorLimbo = false;
        setCursorPosToCurr();
    }

    public void shiftCursorYPos(double x){
        this.cursorYPos += x;
    }


    private boolean stop(Node i, int line){
        //sees where iteration of a line should stop
        if (lineList.size() - 1 > line){
            return i != end && i != lineList.get(line + 1);
        } else {
            return i != end;
        }
    }



    public void restructure(){
        //restructures the data structure. ie sets the linelist to appropriate values
        double wordLength = margin;
        
        int line = 0;
        start.item.setFont(Font.font(fontName, fontSize));
        Text testText = new Text(" ");
        testText.setFont(Font.font(fontName, fontSize));
        double whiteSpaceWidth = width(testText);
        int whiteSpaceCounter = 0;
        Node i = start.next;
        startWord = i;
        lineList = new ArrayList<Node>();
        double w = margin;
        lineList.add(0, i);
        while (i != end){
            i.item.setFont(Font.font(fontName, fontSize));
            w += width(i.item);
            wordLength += width(i.item);
            if (w + whiteSpaceWidth > windowWidth - marginRight && i.item.getText().equals(" ")){
                i = i.next;
            } else {

                if (i.item.getText().equals(" ")){
                    startWord = i.next;
                    wordLength = margin;
                }
                if (wordLength > windowWidth - marginRight){
                    lineList.add(line + 1, i);                                 
                    line++;
                    w = margin + width(i.item);
                    wordLength = margin + width(i.item);
                    startWord = i;
                } else if (w > windowWidth -marginRight){
                    lineList.add(line + 1, startWord);                                 
                    line++;
                    wordLength = margin + width(i.item);
                    w = margin + width(i.item);
                    i = lineList.get(line);
                    startWord = i;
                }
                if (i.item.getText().equals("\n") || i.item.getText().equals("\r\n")){
                    lineList.add(line + 1, i);                                 
                    line++;
                    w = margin + width(i.item);
                    wordLength = margin;
                    startWord = lineList.get(line);
                }
                if (i == curr){currLine = line;}
                i = i.next;
            }
        }
    }


    public double widthLine(int num){
        //gets the width of a line
        Node n = lineList.get(num);
        double width = margin;
        if (lineList.size() - 1 - num > 1){
            while (n != end && n != lineList.get(num + 1)){
                width += width(n.item);
                n = n.next;
            }
        } else {

            while (n != end && n.item != null){
                width += width(n.item);
                n = n.next;
            }
        }
        return width;
    }





    public void shiftCurrToPlaceInLine(int place){
        //given a position in a certain line, curr gets shifted there
        Node i = lineList.get(currLine);
        while (place != 0 && i.next != end){
            i = i.next;
            place --;
        }
        curr = i;
    }

    public int currPlaceInLine(){
        //returns the place of where curr is in line
        Node i = lineList.get(currLine);
        int count = 0;
        while (i != curr){
            count ++;
            i = i.next;
        }
        return count;
    }

    public Text peekCurr(){
        if (N == 0){
            throw new RuntimeException("you tried to remove from empty dumbo");
        }
        return curr.item;
    }

    public Action undo(){
        //undos the last action
        if (undoStack.size() > 0){
            Action action = undoStack.pop();
            if (action.getAdd()){
                currLine = action.getLine();
                shiftCurrToPlaceInLine(action.getPlace());
                removeCurr(true);
            } else {
                currLine = action.getLine();
                shiftCurrToPlaceInLine(action.getPlace());
                addAtCurr(action.getItem(), true);
            }
            action.setLine(currLine);
            redoStack.push(action);
            setCursorPosToCurr(); //This part lags a bit for some reason. If ctrl z is pressed immediately after ctrl y, this part will lag
            return action;
        } else {
            return null;
        }       
    }

    public Action redo(){
        //undos the last action
        if (redoStack.size() > 0){
            Action action = redoStack.pop();
            if (!action.getAdd()){
                currLine = action.getLine();
                shiftCurrToPlaceInLine(action.getPlace());
                removeCurr(true);
            } else {
                currLine = action.getLine();
                shiftCurrToPlaceInLine(action.getPlace() - 1);
                addAtCurr(action.getItem(), true);
            }
            action.setLine(currLine);
            undoStack.push(action);
            setCursorPosToCurr();
            return action;
        } else {
            return null;
        }
        
    }

    public void addAtCurr(String chara) {
        //adds a text object of chara at curr and pushes this action on the undo stack
    	Text text = new Text(chara);
    	text.setTextOrigin(VPos.TOP);  	
    	text.setX(cursorXPos);
        double w = width(text);
    	Node x = new Node();
    	if (this.isEmpty()){
    		x.item = text;
    		curr = x;
    		curr.next = end;
    		curr.prev = start;
    		start.next = curr;
    		end.prev = curr;
    	} else{
    		x.item = text;
	        x.next = curr.next;
	        x.prev = curr;
	        curr.next.prev = x;
	        curr.next = x;
	        curr = curr.next;
    	}
    	N++;
        rerender();
        undoStack.push(new Action(x, true, currLine, currPlaceInLine()));     	
    }

    public void addAtCurr(Text text, Boolean undoing) {
        //same as before but used in undoing so that this action doesn't get pushed onto the undo stack
        text.setTextOrigin(VPos.TOP);   
        text.setX(cursorXPos);
        double w = width(text);
        Node x = new Node();
        if (this.isEmpty()){
            x.item = text;
            curr = x;
            curr.next = end;
            curr.prev = start;
            start.next = curr;
            end.prev = curr;
        } else{
            x.item = text;
            x.next = curr.next;
            x.prev = curr;
            curr.next.prev = x;
            curr.next = x;
            curr = x;
        }
        N++;
        rerender();
    }



    public Text removeCurr(){
        //removes the current element and pushes this action on the undo stack
    	if (N == 0){
    		throw new RuntimeException("you tried to remove from empty dumbo");
    	}
        if (this.curr != start){
            Node i = curr;
            Action pushAction = new Action(i, false, currLine, currPlaceInLine());
            undoStack.push(pushAction);
            Text temp = curr.item;
            curr.prev.next = curr.next;
            curr.next.prev = curr.prev;
            curr = curr.prev;
            N--;
            rerender();
            pushAction.setLine(currLine);
            
            setCursorPosToCurr();
            return temp;
        } else {
            rerender();
            return new Text("");
        }

    }

    public Text removeCurr(Boolean undoing){
        //same as the above method but does not push the action onto the stack
        if (N == 0){
            throw new RuntimeException("you tried to remove from empty");
        }
        if (this.curr != start){
            Node i = curr;
            Text temp = curr.item;
            curr.prev.next = curr.next;
            curr.next.prev = curr.prev;
            curr = curr.prev;
            N--;
            rerender();
            return temp;
        } else {
            rerender();
            return new Text("");
        }

    }

    public void setCursorPosToCurr(){
        //moves the cursor position to the appropriate place given the curr value
        if (curr == start){
            setCursorPos(margin, 0);
        } else {
            double xPos = margin;
            Node i = lineList.get(currLine);
            while (i != curr.next){
                xPos += width(i.item);
                i = i.next;

            }

            
            setCursorPos(xPos, changeIndexToYPos(currLine) + scrollShift);
        }
    }



    public void moveUp(){
        //moves cursor and curr one place up
        if(curr.next != end){
            if (cursorLimbo){
                curr = curr.next;
                currLine ++;
                setCursorPosToCurr();
                cursorLimbo = false;
            
            } else if (hasNextLine() && curr.next == lineList.get(currLine + 1) && !curr.next.item.getText().equals("\n")){
                setCursorPos(margin, changeIndexToYPos(currLine + 1));
                cursorLimbo = true;
            }else if (curr.next.item.getText().equals("\n")){
                currLine ++;
                curr = curr.next;
                setCursorPosToCurr();
            } else {
                curr = curr.next;
                setCursorPosToCurr();
            }
        }
    }

    public void moveDown(){
        //moves curosr and curr one place down

        if(curr != start){
            if (cursorLimbo){             
                setCursorPosToCurr();
                cursorLimbo = false;
            } else if (curr == lineList.get(currLine) && !curr.item.getText().equals("\n") && curr.prev != start){
                curr = curr.prev;
                currLine --;
                setCursorPos(margin, changeIndexToYPos(currLine + 1));
                cursorLimbo = true;
            }else if (curr.item.getText().equals("\n") && curr.prev != start){
                currLine --;
                curr = curr.prev;
                setCursorPosToCurr();
            } else {
                if (curr.item.getText().equals("\n")){currLine --;}
                curr = curr.prev;
                setCursorPosToCurr();
            }      
        }

    }



    public boolean hasNextLine(){
        return lineList.size() > currLine + 1;
    }

    

    public Text getCurrItem(){
    	return curr.item;
    }


    public void setCursorPos(double x, double y){
        this.cursorXPos = x;
        this.cursorYPos = y;
    }

    public static double width(Text text){
        if (text == null || text.getText() == "\n"){
            return 0;
        } else {
            return (double) Math.round(text.getLayoutBounds().getWidth());
        }
    }

    public static double height(Text text){
    	return (double) Math.round(text.getLayoutBounds().getHeight());
    }

    public double changeIndexToYPos(int i){
        //given an index of the linelist (ie a line) changes it to the appropriate y position
        double height = height(this.start.item);
        return height * (double) i;
    }

    public int changeYPosToIndex(double y){
        //vice versa of the above
        return (int) Math.floor(y / height(this.start.item));
    }

    public void mouseClickMove(double x, double y){
        //shifts the cursor after a mouse click
        y -= scrollShift;
        if (x < margin){
            x = 0;
        }
        moveTo(x, changeYPosToIndex(y));
    }

    public void upArrow(){
        if (cursorLimbo && currLine != 0){
            currLine --;
            curr = lineList.get(currLine + 1).prev;
            setCursorPos(margin, changeIndexToYPos(currLine + 1));
        } else if (currLine == 0 && cursorLimbo){
            curr = start;
            setCursorPosToCurr();
            cursorLimbo = false;
        } else {
            moveTo(getCursorXPos(), Math.max(getCurrLine() - 1, 0));
        }
    }

    public void downArrow(){
        if (cursorLimbo && lineList.size() - currLine > 1){
            currLine ++;
            curr = lineList.get(currLine + 1).prev;
            setCursorPos(margin, changeIndexToYPos(currLine + 1));
        } else {
            moveTo(getCursorXPos(), Math.min(getCurrLine() + 1, highestIndex()));
        }
    }


    public void moveTo(double x, int y){
        //moves the cursor to a x position and a line y
        y = Math.min(y, lineList.size() - 1);
        currLine = y;
        Node i = lineList.get(y);
        if (x <= margin){
            curr = i;
            setCursorPosToCurr();
        } else if (i.item == null){

        } else {
            double w1 = margin;
            double w2 = margin;
            while (w1 + width(i.item) < x){
                w1 += width(i.item);
                if (!stop(i.next, y)){
                    curr = i;
                    setCursorPosToCurr();
                    break;
                }
                i = i.next;
            }
            curr = i;
            setCursorPosToCurr();
            w2 = w1 + width(i.item);
            if (Math.abs(w2 - x) < Math.abs(w1 - x)){
                curr = i;
                setCursorPosToCurr();
            } else {
                if (!curr.item.getText().equals("\n")){
                    curr = i.prev;
                }
                setCursorPosToCurr();
            }
        }
        
        
    }

    public void shiftUpByW(Double w, Node i){
        while (i != end){
            i.item.setX(i.item.getX() + w);
            i = i.next;
        }
    }

    public void print(String str){
        if (this.debug){
            System.out.println(str);
        }
    }
}

    

