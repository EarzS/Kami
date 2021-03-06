package list;

import java.util.Iterator;

public class LList<T> implements Iterable<LList<T>> {
	private LList<T> next, prev;
	public T node;
	
	public LList() {
	}
	
	private LList(LList<T> next, LList<T> prev, T node) {
		this.next = next;
		this.prev = prev;
		this.node = node;
	}
	
	// adds to front
	public LList<T> add(T node) {
		LList<T> r = new LList<>(this, null, node);
		this.prev = r;
		return r;
	}
	
	// returns head.next if removing head
	public LList<T> remove(LList<T> lList) {
		if (lList.next != null)
			lList.next.prev = lList.prev;
		if (lList.prev != null)
			lList.prev.next = lList.next;
		if (this == lList)
			return next;
		return this;
	}
	
	public Iterator<LList<T>> iterator() {
		return new LListIterator();
	}
	
	private class LListIterator implements Iterator<LList<T>> {
		LList<T> cur;
		
		LListIterator() {
			cur = LList.this;
		}
		
		public boolean hasNext() {
			return cur != null && cur.node != null;
		}
		
		public LList<T> next() {
			LList<T> r = cur;
			cur = cur.next;
			return r;
		}
	}
}
