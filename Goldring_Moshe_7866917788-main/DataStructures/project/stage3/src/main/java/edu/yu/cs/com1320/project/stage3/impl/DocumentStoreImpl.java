package edu.yu.cs.com1320.project.stage3.impl;


import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, Document> table;
    private StackImpl<Undoable> commandStack;
    private TrieImpl<Document> tree;

    public DocumentStoreImpl(){
        this.table = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.tree = new TrieImpl<>();
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
                commandStack.push(new GenericCommand(uri, (u) -> {
                    this.table.put((URI)u, null);
                    return true;
                }));
                return 0;
            }else{
                //essentially a delete
                Document doc = getDocument(uri);
                for(String word: doc.getWords()){
                    tree.delete(word, doc);
                }
                Document previous = this.table.put(uri, null);
                commandStack.push(new GenericCommand(uri, (u) -> {
                    this.table.put((URI)u, previous);
                    this.TriePut(previous);
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
            TriePut(this.table.get(uri));
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc = getDocument(uri);
                for(String word: doc.getWords()){
                    tree.delete(word, doc);
                }
                this.table.put((URI)u, null);
                return true;
            }));
            return 0;
        }else {
            Document doc = getDocument(uri);
            for(String word: doc.getWords()){
                tree.delete(word, doc);
            }
            Document previous = this.table.put(uri, document);
            TriePut(this.table.get(uri));
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc2 = getDocument(uri);
                this.table.put((URI)u, previous);
                for(String word: doc2.getWords()){
                    tree.delete(word, doc2);
                }
                TriePut(previous);
                return true;
            }));
            return previous.hashCode();
        }
    }

    private void TriePut(Document previous){
        Document doc = previous;
        for(String word: doc.getWords()){
            tree.put(word, doc);
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
            Document doc = getDocument(uri);
            for(String word: doc.getWords()){
                tree.delete(word, doc);
            }
            Document previous = this.table.put(uri, null);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.table.put((URI)u, previous);
                TriePut(previous);
                return true;
            }));
            return true;
        }else{
            Document current = this.table.get(uri);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.table.put((URI)u, current);
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
            commandStack.pop().undo();
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
        StackImpl<Undoable> tempStack = new StackImpl<>();
        while(this.commandStack.peek() != null) {
            if (commandStack.peek() instanceof GenericCommand) {
                GenericCommand command = (GenericCommand) this.commandStack.peek();
                if (command.getTarget().equals(uri)) {
                    break;
                } else {
                    GenericCommand tempCommand = (GenericCommand) commandStack.pop();
                    tempStack.push(tempCommand);
                    if(this.commandStack.size() == 0){
                        while(tempStack.size() != 0){
                            this.commandStack.push(tempStack.pop());
                        }
                        throw new IllegalStateException();
                    }
                }
            } else {
                CommandSet commandSet = (CommandSet) this.commandStack.peek();
                if (commandSet.containsTarget(uri)) {
                    break;
                } else {
                    CommandSet tempCommandSet = (CommandSet) commandStack.pop();
                    tempStack.push(tempCommandSet);
                }
            }
        }
        if(this.commandStack.peek() != null){
            if (commandStack.peek() instanceof GenericCommand) {
                commandStack.pop().undo();
            }else {
               CommandSet<Object> undoneCommand = (CommandSet) this.commandStack.pop();
               undoneCommand.undo(uri);
               if(!undoneCommand.isEmpty()) {
                   this.commandStack.push(undoneCommand);
               }
            }
        }else{
            if(this.commandStack.size() == 0){
                while(tempStack.size() != 0){
                    this.commandStack.push(tempStack.pop());
                }
                throw new IllegalStateException();
            }
        }
        while(tempStack.size() != 0){
            this.commandStack.push(tempStack.pop());
        }
    }

    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> search(String keyword) {
        String newKeyword = keyword.replace("[^A-Za-z0-9]", "").toUpperCase();
        return this.tree.getAllSorted(keyword, (doc1, doc2) -> {
            if (doc1.wordCount(newKeyword) > doc2.wordCount(newKeyword)) {
                return -1;
            } else if (doc1.wordCount(newKeyword) < doc2.wordCount(newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        });
    }
    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        String newKeyword = keywordPrefix.replace("[^A-Za-z0-9]", "").toUpperCase();
        return this.tree.getAllWithPrefixSorted(keywordPrefix, ((doc1, doc2) -> {
            if (preFixCount(doc1, newKeyword) > preFixCount(doc2, newKeyword)) {
                return -1;
            } else if (preFixCount(doc1, newKeyword) < preFixCount(doc2, newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        }));
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<Document> deletions = tree.deleteAll(keyword);
        Set<URI> returnSet = new HashSet<>();
        for(Document doc: deletions){
            if(getDocument(doc.getKey()) != null){
                returnSet.add(doc.getKey());
            }
        }
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        for(Document doc: deletions){
            URI uri = doc.getKey();
            Document previous = this.table.put(uri, null);
            Function<URI,Boolean> undo = (URI u) ->{
                this.table.put(u, previous);
                for(String word: previous.getWords()){
                    this.tree.put(word, previous);
                }
                return true;
            };
            GenericCommand command = new GenericCommand<>(uri, undo);
            if(!commandSet.containsTarget(command)){
                commandSet.addCommand(command);
            }
        }
        commandStack.push(commandSet);
        return returnSet;
    }

    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        Set<Document> deletions = tree.deleteAllWithPrefix(keywordPrefix);
        Set<URI> returnSet = new HashSet<>();
        for(Document doc: deletions){
            returnSet.add(doc.getKey());
        }
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        for(Document doc: deletions){
            URI uri = doc.getKey();
            Document previous = this.table.put(uri, null);
            Function<URI,Boolean> undo = (URI u) ->{
                this.table.put(u, previous);
                for(String word: previous.getWords()){
                    this.tree.put(word, previous);
                }
                return true;
            };
            GenericCommand command = new GenericCommand<>(uri, undo);
            if(!commandSet.containsTarget(command)){
                commandSet.addCommand(command);
            }
        }
        commandStack.push(commandSet);
        return returnSet;
    }

    private int preFixCount (Document doc, String prefix){
        int counter = 0;
        for(String word: doc.getWords()){
            if(word.startsWith(prefix)){
                counter += 1;
            }
        }
        return counter;
    }
}
