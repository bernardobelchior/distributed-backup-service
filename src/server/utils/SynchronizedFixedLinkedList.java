package server.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        if (list.size() >= maxSize)
            return false;

        list.add(element);
        return true;
    }

    public boolean add(int index, T element) {
        if (list.size() >= maxSize)
            return false;

        list.add(index, element);
        return true;
    }

    public void set(int index, T element) {
        list.set(index, element);
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public boolean remove(Object object) {
        System.err.println("Successor removed!!!!! " + object.toString());
        return list.remove(object);
    }

    public T last() {
        return list.get(list.size() - 1);
    }
}
