package edu.yu.cs.com1320.project.stage3.impl;

import edu.yu.cs.com1320.project.stage3.Document;

import java.net.URI;
import java.util.*;
//stage3
public class DocumentImpl implements Document {
    private URI key;
    private String text;
    private byte[] bytes;
    private Map<String, Integer> wordCounter;
    private boolean isTxtDoc;

    public DocumentImpl(URI uri, String txt){
        if(uri == null || txt == null){
            throw new IllegalArgumentException();
        }else if(uri.toString().isEmpty() || txt.isBlank()) throw new IllegalArgumentException();
        this.key = uri;
        this.text = txt;
        this.isTxtDoc = true;
        this.wordCounter = new HashMap<>();
        for(String word: txt.split(" ")){
            word = word.replaceAll("[^A-Za-z0-9]", "");
            word = word.toUpperCase();
            if(!wordCounter.containsKey(word)){
                wordCounter.put(word, 1);
            }else{
                wordCounter.put(word, wordCounter.get(word) + 1);
            }
        }
    }

    public DocumentImpl(URI uri, byte[] binaryData){
        if(uri == null || binaryData == null){
            throw new IllegalArgumentException();
        }else if(uri.toString().isEmpty() || binaryData.length == 0) throw new IllegalArgumentException();
        this.key = uri;
        this.bytes = binaryData;
        this.isTxtDoc = false;
    }

    /**
     * @return content of text document
     */
    @Override
    public String getDocumentTxt() {
        return this.text;
    }

    /**
     * @return content of binary data document
     */
    @Override
    public byte[] getDocumentBinaryData() {
        return this.bytes;
    }

    /**
     * @return URI which uniquely identifies this document
     */
    @Override
    public URI getKey() {
        return this.key;
    }

    /**
     * how many times does the given word appear in the document?
     * @param word
     * @return the number of times the given words appears in the document. If it's a binary document, return 0.
     */
    @Override
    public int wordCount(String word) {
        word = word.replaceAll("[^A-Za-z0-9]", "");
        word = word.toUpperCase();
        if(!isTxtDoc || !wordCounter.containsKey(word)){
            return 0;
        }else{
            return wordCounter.get(word);
        }
    }



    /**
     * @return all the words that appear in the document
     */
    @Override
    public Set<String> getWords() {
        if(isTxtDoc){
            return this.wordCounter.keySet();
        }else{
            return new HashSet<>();
        }
    }

    @Override
    public boolean equals(Object o){
        if(this == o) {
            return true;
        }
        //see if it's null
        if(o == null) {
            return false;
        }
        //see if they're from the same class
        if(!(o instanceof Document)){
            return false;
        }
        Document otherDocument = (Document) o;
        return this.key.hashCode() == otherDocument.getKey().hashCode();
    }

    @Override
    public int hashCode() {
        int result = this.key.hashCode();
        result = 31 * result + (this.text != null ? this.text.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(this.bytes);
        return result;
    }
}
