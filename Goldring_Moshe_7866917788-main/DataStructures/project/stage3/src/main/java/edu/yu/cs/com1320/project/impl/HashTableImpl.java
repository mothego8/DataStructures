package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

//stage3
public class HashTableImpl<Key,Value> implements HashTable<Key,Value> {
    /**
     * Instances of HashTable should be constructed with two type parameters, one for the type of the keys in the table and one for the type of the values
     * @param <Key>
     * @param <Value>
     */
    class Node <Key, Value>{
        Key k;
        Value v;
        Node<Key, Value> next;

        public Node(Key k, Value v, Node<Key, Value> n){
            this.k = k;
            this.v = v;
            this.next = n;
        }

        private Key getKey(){
            return this.k;
        }

        private Value getValue(){
            return this.v;
        }

        private void setValue(Value v){
            this.v = v;
        }
    }

    private Node<Key, Value>[] entries;
    private int numOfEntries;
    final double DEFAULT_LOAD_FACTOR = 0.75;

    public HashTableImpl(){
        this.entries = new Node[5];
        this.numOfEntries = 0;
    }

    /**
     //@param key
     * @param k the key whose value should be returned
     * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
     */

    @Override
    public Value get(Key k) {
        int index = hashFunction(k);
        for(Node<Key, Value> node = this.entries[index]; node != null; node = node.next){
            if(k.equals(node.getKey())){
                return node.getValue();
            }
        }
        return null;
    }

    /**
     * @param k the key at which to store the value
     * @param v the value to store.
     * To delete an entry, put a null value.
     * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
     */
    @Override
    public Value put(Key k, Value v) {
        double loadFactor = (numOfEntries * 1.0)/this.entries.length;
        if(loadFactor > DEFAULT_LOAD_FACTOR){
            rehash();
        }
        int index = hashFunction(k);
        for(Node<Key, Value> node = this.entries[index]; node != null; node = node.next){
            if(k.equals(node.getKey())){
                Value old = node.getValue();
                node.setValue(v);
                return old;
            }
        }
        Node<Key, Value> newNode = new Node<>(k, v, this.entries[index]);
        this.entries[index] = newNode;
        this.numOfEntries += 1;
        return null;
    }

    private int hashFunction(Object key){
        return (key.hashCode() & 0x7fffffff) % this.entries.length;
    }

    private void rehash(){
        this.numOfEntries = 0;
        Node<Key,Value> [] temp = this.entries;
        this.entries = new Node[this.entries.length * 2];
        for(int i = 0; i < temp.length; i++){
            Node<Key,Value> node = temp[i];
            while(node != null){
                Key k = node.getKey();
                Value v = node.getValue();
                put(k, v);
                node = node.next;
            }
        }
    }

}
