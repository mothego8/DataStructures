package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.HashTable;

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

        private Value setValue(Value v){
            return this.v = v;
        }
    }

    private Node<Key, Value>[] entries;

    public HashTableImpl(){
        this.entries = new Node[5];
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
        int index = hashFunction(k);
        Node<Key, Value> node = this.entries[index];
        while(node != null){
            if(k == node.getKey()){
                Value old = node.getValue();
                node.setValue(v);
                return old;
            }
            node = node.next;
        }
        Node<Key, Value> newNode = new Node<>(k, v, this.entries[index]);
        this.entries[index] = newNode;
        return null;
    }

    private int hashFunction(Object key){
        return (key.hashCode() & 0x7fffffff) % this.entries.length;
    }
}
