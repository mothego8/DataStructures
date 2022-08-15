package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, Document> table;
    private StackImpl<Command> commandStack;

    public DocumentStoreImpl(){
        this.table = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
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
                commandStack.push(new Command(uri, (u) -> {
                    this.table.put(u, null);
                    return true;
                }));
                return 0;
            }else{
                Document previous = this.table.put(uri, null);
                commandStack.push(new Command(uri, (u) -> {
                    this.table.put(u, previous);
                    return true;
                }));
                return previous.hashCode();
            }
        }
        if(uri == null || format == null){
            throw new IllegalArgumentException();
        }
        DocumentImpl document;
        if(format == DocumentFormat.TXT) {
            String inputTxt = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            document = new DocumentImpl(uri, inputTxt);
        }else{
            byte [] inputByte = input.readAllBytes();
            document = new DocumentImpl(uri, inputByte);
        }
        if(this.table.get(uri) == null){
            this.table.put(uri, document);
            commandStack.push(new Command(uri, (u) -> {
                this.table.put(u, null);
                return true;
            }));
            return 0;
        }else {
            Document previous = this.table.put(uri, document);
            commandStack.push(new Command(uri, (u) -> {
                this.table.put(u, previous);
                return true;
            }));
            return previous.hashCode();
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
        if(this.table.get(uri) != null){
            Document previous = this.table.put(uri, null);
            commandStack.push(new Command(uri, (u) -> {
                this.table.put(u, previous);
                return true;
            }));
            return true;
        }else{
            Document current = this.table.get(uri);
            commandStack.push(new Command(uri, (u) -> {
                this.table.put(u, current);
                return true;
            }));
            return false;
        }
    }

    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        if(commandStack.size() == 0){
            throw new IllegalStateException();
        }else {
            Command command = commandStack.pop();
            command.undo();
        }
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    @Override
    public void undo(URI uri) throws IllegalStateException {
        if(this.commandStack.size() == 0){
            throw new IllegalStateException();
        }
        StackImpl<Command> tempStack = new StackImpl<>();
        while(!this.commandStack.peek().getUri().equals(uri)){
            Command command = this.commandStack.pop();
            tempStack.push(command);
            if(this.commandStack.size() == 0){
                while(tempStack.size() != 0){
                    Command tempC = tempStack.pop();
                    this.commandStack.push(tempC);
                }
                throw new IllegalStateException();
            }
        }
        Command command = commandStack.pop();
        command.undo();
        while(tempStack.size() != 0){
            Command tempCommand = tempStack.pop();
            this.commandStack.push(tempCommand);
        }
    }
}
