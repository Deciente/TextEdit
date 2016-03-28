package editor;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import editor.TextStorage.Node;


public class Action{
	private boolean add;
	private Text item;
	private Node prev;
	private Node next;
	private int line;
	private Node node;
	private int placeInLine;

	public Action(Node i, boolean add, int line, int place){
		this.node = i;
		this.add = add;
		this.item = i.getItem();
		this.prev = i.getPrev();
		this.next = i.getNext();
		this.line = line;
		this.placeInLine = place;
	}

	public Text getItem(){
		return this.item;
	}

	public void setLine(int line){
		this.line = line;
	}

	public boolean getAdd(){
		return this.add;
	}

	public Node prev(){
		return this.prev;
	}

	public Node next(){
		return this.next;
	}

	public int getLine(){
		return this.line;
	}

	public Node node(){
		return this.node;
	}

	public int getPlace(){
		return this.placeInLine;
	}
}
