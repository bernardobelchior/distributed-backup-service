package server.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SynchronizedFixedLinkedList<T> {
    private final List<T> list;
    private final int maxSize;

    public SynchronizedFixedLinkedList(int maxSize) {
        this.maxSize = maxSize;
        list = Collections.synchronizedList(new LinkedList<T>());
    }

    public T get(int index) {
        return list.get(index);
    }

    public int size() {
        return list.size();
    }

    public boolean add(T element) {
        if (list.size() < maxSize) {
            list.add(element);
            return true;
        }

        return false;
    }

    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    public void set(int index, T element) {
        list.set(index, element);
    }

    public void popLast() {
        list.remove(list.size() - 1);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
}
