package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DocumentStoreImpl implements DocumentStore {
    HashTableImpl<URI, Document> table;

    public DocumentStoreImpl(){
        this.table = new HashTableImpl<>();
    }

    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if uri or format are null
     */
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if(input == null){
            if(this.table.get(uri) == null){
                this.table.put(uri, null);
                return 0;
            }else{
                int hash = this.table.get(uri).hashCode();
                this.table.put(uri, null);
                return hash;
            }
        }
        if(uri == null || format == null){
            throw new IllegalArgumentException();
        }
        if(format == DocumentFormat.TXT) {
            String inputTxt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            DocumentImpl txt = new DocumentImpl(uri, inputTxt);
            if(this.table.get(uri) == null){
                this.table.put(uri, txt);
                return 0;
            }else{
                int hash = this.table.get(uri).hashCode();
                this.table.put(uri, txt);
                return hash;
            }
        }else{
            byte [] inputByte = input.readAllBytes();
            DocumentImpl bytes = new DocumentImpl(uri, inputByte);
            if(this.table.get(uri) == null){
                this.table.put(uri, bytes);
                return 0;
            }else{
                int hash = this.table.get(uri).hashCode();
                this.table.put(uri, bytes);
                return hash;
            }
        }
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document getDocument(URI uri) {
        return this.table.get(uri);
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if(this.table.get(uri) == null){
            return false;
        }
        this.table.put(uri, null);
        return true;
    }
}
