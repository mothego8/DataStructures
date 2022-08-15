package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.stage2.Document;

import java.net.URI;
import java.util.Arrays;

public class DocumentImpl implements Document {
    private URI key;
    private String text;
    private byte[] bytes;

    public DocumentImpl(URI uri, String txt){
        if(uri == null || txt == null){
            throw new IllegalArgumentException();
        }else if(uri.toString().isEmpty() || txt.isBlank()) throw new IllegalArgumentException();
        this.key = uri;
        this.text = txt;
    }

    public DocumentImpl(URI uri, byte[] binaryData){
        if(uri == null || binaryData == null){
            throw new IllegalArgumentException();
        }else if(uri.toString().isEmpty() || binaryData.length == 0) throw new IllegalArgumentException();
        this.key = uri;
        this.bytes = binaryData;
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
