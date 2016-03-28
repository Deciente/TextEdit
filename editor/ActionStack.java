package editor;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import editor.TextStorage.Node;

public class ActionStack<T>{
	private int size;
	private Node top;
	private int capacity = 100;
	public Node start;
	public Node end;

	private class Node{
		private Node next;
		private Node prev;
		private T item;
	}

	public ActionStack(){
		size = 0;
		start = new Node();
		end = new Node();
		start.next = end;
		end.prev = start;
		top = start;
	}

	public void push(T a){
		if (size < capacity){
			Node node = new Node();
			node.item = a;
			node.prev = top;
			top.next = node;
			top = node;
			top.next = end;
			end.prev = top;
			size ++;
		} else {
			start.next.next.prev = start;
			start.next = start.next.next;
			Node node = new Node();
			node.item = a;
			node.prev = top;
			top.next = node;
			top = node;
			top.next = end;
			end.prev = top;
		}
	}

	public T pop(){
		size --;
		T popped = top.item;
		top.prev.next = null;
		top = top.prev;
        return popped;
	}

	public T peek(){
		return top.item;
	}

	public int size(){
		return this.size;
	}
	
}