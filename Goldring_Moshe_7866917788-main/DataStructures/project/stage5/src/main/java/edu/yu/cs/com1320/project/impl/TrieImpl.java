package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {

    private static final int alphabetSize = 36; // extended ASCII
    private Node root; // root of trie

    class Node<Value>
    {
        private List <Value> valList = new ArrayList<>();
        protected Node[] links = new Node[alphabetSize];

    }

    public TrieImpl(){
        this.root = new Node();
    }

    /**
     * add the given value at the given key
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if(key == null){
            throw new IllegalArgumentException();
        }
        //deleteAll the value from this key
        else {
            this.root = put(this.root, key, val, 0);
        }
    }

    private Node put(Node x, String key, Value val, int d){
        key = key.toUpperCase();
        //create a new node
        if (x == null) {
            x = new Node();
        }
        //we've reached the last node in the key,
        //set the value for the key and return the node
        if (d == key.length()) {
            if(!x.valList.contains(val)){
                x.valList.add(val);
            }
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        if(c >= 'A'){
            x.links[c - 'A'] = this.put(x.links[c - 'A'], key, val, d + 1);
        }else{
            x.links[c - 22] = this.put(x.links[c - 22], key, val, d + 1);
        }
        return x;
    }

    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */
    @Override
    public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
        if(key == null){
            throw new IllegalArgumentException();
        }
        if(comparator == null){
            throw new IllegalArgumentException();
        }
        //not sure what to do here
        /*if(key == ""){
            return new ArrayList<>();
        }*/
        key = key.toUpperCase();
        Node x = this.get(this.root, key, 0);
        if (x == null) {
            return new ArrayList<>();
        }
        List<Value> sortedList = x.valList;
        sortedList.sort(comparator);
        return sortedList;
    }

    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
        if(prefix == null){
            throw new IllegalArgumentException();
        }
        if(comparator == null){
            throw new IllegalArgumentException();
        }
        //not sure what to do here
        /*if(prefix == ""){
            return new ArrayList<>();
        }*/
        prefix = prefix.toUpperCase();
        List<String> sortedPreList = new ArrayList<>();
        List<Value> listToReturn = new ArrayList<>();
        Set<Value> set = new HashSet<>();
        collect(get(root,prefix,0), prefix, sortedPreList);
        //make list of strings into list of values
        for(String word: sortedPreList){
            //add to set to prevent duplicates
            set.addAll(getAllSorted(word, comparator));
        }
        listToReturn.addAll(set);
        listToReturn.sort(comparator);
        return listToReturn;
    }

    /**
     * Delete the subtree rooted at the last character of the prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if(prefix == null){
            throw new IllegalArgumentException();
        }
        prefix = prefix.toUpperCase();
        List<String> sortedPreList = new ArrayList<>();
        Set<Value> setToReturn = new HashSet<>();
        collect(get(root,prefix,0), prefix, sortedPreList);
        //make list of strings into list of values
        for(String word: sortedPreList){
            setToReturn.addAll(deleteAll(word));
        }
        return setToReturn;
    }

    /**
     * Delete all values from the node of the given key (do not remove the values from other nodes in the Trie)
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @Override
    public Set<Value> deleteAll(String key) {
        if(key == null){
            throw new IllegalArgumentException();
        }
        key = key.toUpperCase();
        Set<Value> deletions = new HashSet<>();
        Node node = get(root, key, 0);
        if(node == null){
            return deletions;
        }
        deletions.addAll(node.valList);
        root = delete(root, key, 0);
        return deletions;
    }

    //private delete
    private Node delete(Node x, String key, int d){
        if(x == null){
            return null;
        }
        if(d == key.length()){
            x.valList.clear();
        }else{
            char c = key.charAt(d);
            if(c >= 'A'){
                x.links[c - 'A'] = delete(x.links[c - 'A'], key, d + 1);
            }else{
                x.links[c - 22] = delete(x.links[c - 22], key, d + 1);
            }
        }
        if(!x.valList.isEmpty()){
            return x;
        }
        for(int c = 0; c < alphabetSize; c++){
            if(x.links[c] != null){
                return x;
            }
        }
        return null;
    }

    /**
     * Remove the given value from the node of the given key (do not remove the value from other nodes in the Trie)
     * @param key
     * @param val
     * @return the value which was deleted. If the key did not contain the given value, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if(key == null){
            throw new IllegalArgumentException();
        }
        if(val == null){
            throw new IllegalArgumentException();
        }
        key = key.toUpperCase();
        Node node = get(root, key, 0);
        Set<Value> loopVal = new HashSet<>(node.valList);
        Value returnVal;
        for(Value v: loopVal){
            if(v.equals(val)){
                returnVal = v;
                node.valList.remove(returnVal);
                if(node.valList.isEmpty()){
                    deleteAll(key);
                }
                return returnVal;
            }
        }
        //root = delete(root, key, val, 0);
        return null;
    }

    private Node get(Node x, String key, int d){
        String newKey = key.toUpperCase();
        //link was null - return null, indicating a miss
        if (x == null) {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == newKey.length()) {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = newKey.charAt(d);
        if(c >= 'A'){
            return this.get(x.links[c - 'A'], key, d + 1);
        }else{
            return this.get(x.links[c - 22], key, d + 1);
        }
    }

    private void collect(Node x, String pre, List<String> l){
        if(x == null){
            return;
        }
        if(!x.valList.isEmpty()){
            if (!l.contains(pre) && pre != null) {
                l.add(pre);
            }
        }
        for(int c = 0; c < alphabetSize; c++){
            if(c < 26){
                char d = (char)(c + 65);
                collect(x.links[c], pre + d, l);
            }else{
                int p = c - 26;
                collect(x.links[c], pre + p, l);
            }
        }
    }
}
