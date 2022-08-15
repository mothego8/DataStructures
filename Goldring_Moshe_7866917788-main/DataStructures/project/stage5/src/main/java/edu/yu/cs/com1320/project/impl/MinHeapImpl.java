package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.impl.BTreeImpl;

import java.net.URI;
import java.util.NoSuchElementException;

//fix new
public class MinHeapImpl<E extends Comparable<E>> extends MinHeap<E> {

    protected BTreeImpl<URI, Document> bTree;
    public MinHeapImpl(){
        elements = (E[]) new Comparable[10];
    }

    public  MinHeapImpl(BTreeImpl<URI, Document> bTree){
        this.bTree = bTree;
        elements = (E[]) new Comparable[10];
    }

    @Override
    public void reHeapify(E element) {
        MinHeapImpl<E> heap = new MinHeapImpl<>(this.bTree);
        for(E e : this.elements){
            if(e != null){
                heap.insert(e);
            }
        }
        for(int i = 1; i <= this.count; i++){
            this.elements[i] = heap.remove();
        }
    }

    @Override
    protected int getArrayIndex(E element) {
        for(int i = 1; i <= this.count; i++){
            if(this.elements[i].equals(element)){
                return i;
            }
        }
        throw new NoSuchElementException("no such element");
    }


    @Override
    protected void doubleArraySize() {
        E[] doubleElements = (E[]) new Comparable[this.elements.length * 2];
        E[] tempElements = this.elements;
        for(int i = 0; i < this.elements.length; i++){
            doubleElements[i] = tempElements[i];
        }
        this.elements = doubleElements;
    }

    @Override
    protected boolean isGreater(int i, int j){
        return(bTree.get((URI) this.elements[i]).compareTo(bTree.get((URI) this.elements[j])) > 0);
    }
}
