package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {

    private int size = 0;
    private Object [] elements;
    private int top = -1;


    public StackImpl(){
        this.elements = new Object[5];
    }

    /**
     * @param element object to add to the Stack
     */
    @Override
    public void push(T element) {
        if(size == this.elements.length){
            int newSize = elements.length * 2;
            Object [] a = new Object[newSize];
            for(int i = 0; i < this.elements.length; i++){
                a[i] = this.elements[i];
            }
            this.elements = a;
        }
        top++;
        size++;
        this.elements[top] = element;
    }

    /**
     * removes and returns element at the top of the stack
     * @return element at the top of the stack, null if the stack is empty
     */
    @SuppressWarnings("unchecked")
    @Override
    public T pop() {
        if(size == 0){
            return null;
        }
        T item = (T) this.elements[top];
        this.elements[top] = null;
        top--;
        size--;
        return item;
    }

    /**
     *
     * @return the element at the top of the stack without removing it
     */
    @SuppressWarnings("unchecked")
    @Override
    public T peek() {
        if(size <= 0){
            return null;
        }
        return (T) this.elements[top];
    }

    /**
     *
     * @return how many elements are currently in the stack
     */
    @Override
    public int size() {
        return this.size;
    }
}
