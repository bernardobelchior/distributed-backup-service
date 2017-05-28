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
        synchronized (list) {
            if (list.size() >= maxSize)
                return false;

            list.add(element);
        }
        return true;
    }

    public boolean add(int index, T element) {
        if (index >= maxSize)
            return false;

        synchronized (list) {
            list.add(index, element);

            if (list.size() >= maxSize)
                list.remove(list.size() - 1);
        }

        return true;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Removes the object and returns the index it had.
     *
     * @param object Object to remove.
     * @return Index of the removed object, or -1 in the of failure.
     */
    public int remove(Object object) {
        @SuppressWarnings("unchecked")
        T toRemove = (T) object;

        if (toRemove == null)
            return -1;

        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).equals(toRemove)) {
                    list.remove(i);
                    return i;
                }
            }
        }

        return -1;
    }

    public T last() {
        return list.get(list.size() - 1);
    }

    public boolean contains(T element) {
        return list.contains(element);
    }
}
