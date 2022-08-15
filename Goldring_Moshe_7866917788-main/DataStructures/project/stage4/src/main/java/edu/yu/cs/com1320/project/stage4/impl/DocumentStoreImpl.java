package edu.yu.cs.com1320.project.stage4.impl;


import com.sun.jdi.ObjectReference;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.lang.System.nanoTime;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, Document> table;
    private StackImpl<Undoable> commandStack;
    private TrieImpl<Document> tree;
    private MinHeapImpl<Document> heap;
    private int maxDoc = 0;
    private int maxBinary = 0;
    private int docCounter = 0;
    private int currentBytes = 0;

    public DocumentStoreImpl(){
        this.table = new HashTableImpl<>();
        this.commandStack = new StackImpl<>();
        this.tree = new TrieImpl<>();
        this.heap = new MinHeapImpl<>();
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
        if(uri == null || format == null){
            throw new IllegalArgumentException();
        }
        if(input == null){
            this.NullInputPut(null, uri, format);
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
            this.heap.insert(document);
            document.setLastUseTime(nanoTime());
            this.heap.reHeapify(document);
            this.docCounter += 1;
            this.currentBytes += this.getMemoryUsage(document);
            TriePut(this.table.get(uri));
            while(isFull()){
                Document doc = this.heap.remove();
                removeDocFromDocumentStore(doc);
                removeCommand(doc);
            }
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc = getDocument(uri);
                for(String word: doc.getWords()){
                    tree.delete(word, doc);
                }
                this.table.put((URI)u, null);
                removeDocFromHeap(doc);
                this.docCounter -= 1;
                return true;
            }));
            return 0;
        }else {
            Document doc = getDocument(uri);
            for(String word: doc.getWords()){
                tree.delete(word, doc);
            }
            Document previous = this.table.put(uri, document);
            this.heap.insert(document);
            document.setLastUseTime(nanoTime());
            this.heap.reHeapify(document);
            TriePut(this.table.get(uri));
            this.currentBytes -= this.getMemoryUsage(previous);
            this.currentBytes += this.getMemoryUsage(document);
            while(isFull()){
                Document doc2 = this.heap.remove();
                removeDocFromDocumentStore(doc2);
                removeCommand(doc2);
            }
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc2 = getDocument(uri);
                this.table.put((URI)u, previous);
                this.removeDocFromHeap(document);
                this.heap.insert(previous);
                previous.setLastUseTime(nanoTime());
                this.heap.reHeapify(previous);
                for(String word: doc2.getWords()){
                    tree.delete(word, doc2);
                }
                TriePut(previous);
                this.currentBytes += this.getMemoryUsage(previous);
                this.currentBytes -= this.getMemoryUsage(document);
                while(isFull()){
                    Document doc3 = this.heap.remove();
                    removeDocFromDocumentStore(doc3);
                    removeCommand(doc3);
                }
                return true;
            }));
            return previous.hashCode();
        }
    }

    private int NullInputPut(InputStream input, URI uri, DocumentFormat format) throws IOException{
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
            removeDocFromHeap(doc);
            this.docCounter -= 1;
            this.currentBytes -= this.getMemoryUsage(previous);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.table.put((URI)u, previous);
                previous.setLastUseTime(nanoTime());
                this.TriePut(previous);
                this.heap.insert(previous);
                this.heap.reHeapify(previous);
                this.currentBytes += this.getMemoryUsage(previous);
                this.docCounter += 1;
                while(isFull()){
                    Document doc2 = this.heap.remove();
                    removeDocFromDocumentStore(doc2);
                    removeCommand(doc2);
                }
                return true;
            }));
            return previous.hashCode();
        }
    }

    private void TriePut(Document previous){
        Document doc = previous;
        doc.setLastUseTime(nanoTime());
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
        Document doc = this.table.get(uri);
        if(doc != null){
            doc.setLastUseTime(nanoTime());
        }
        this.heap.reHeapify(doc);
        return doc;
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
            removeDocFromHeap(doc);
            this.docCounter -= 1;
            this.currentBytes -= getMemoryUsage(doc);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.table.put((URI)u, previous);
                this.heap.insert(previous);
                previous.setLastUseTime(nanoTime());
                this.heap.reHeapify(previous);
                TriePut(previous);
                this.docCounter += 1;
                this.currentBytes += getMemoryUsage(previous);
                while(isFull()){
                    Document doc2 = this.heap.remove();
                    removeDocFromDocumentStore(doc2);
                    removeCommand(doc2);
                }
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

    private void removeCommand(Document doc) throws IllegalStateException{
        if(this.commandStack.size() == 0){
            throw new IllegalStateException();
        }
        StackImpl<Undoable> tempStack = new StackImpl<>();
        Undoable u = this.commandStack.peek();
        while(u != null){
            if(u instanceof GenericCommand){
                if(((GenericCommand<?>) u).getTarget().equals(doc.getKey())){
                    this.commandStack.pop();
                    break;
                }else{
                    tempStack.push(this.commandStack.pop());
                }
            }else{
                if(((CommandSet<Object>)u).containsTarget(doc.getKey())){
                    CommandSet<Object> commandSet = (CommandSet<Object>)this.commandStack.pop();
                    commandSet.remove(doc.getKey());
                    if(!commandSet.isEmpty()){
                        commandStack.push(commandSet);
                    }
                    break;
                }else{
                    tempStack.push(this.commandStack.pop());
                }
            }
            u = commandStack.peek();
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
        List<Document> sortedList = this.tree.getAllSorted(keyword, (doc1, doc2) -> {
            if (doc1.wordCount(newKeyword) > doc2.wordCount(newKeyword)) {
                return -1;
            } else if (doc1.wordCount(newKeyword) < doc2.wordCount(newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        });
        for(Document doc: sortedList){
            doc.setLastUseTime(nanoTime());
        }
        return sortedList;
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
        List<Document> sortedPreList = this.tree.getAllWithPrefixSorted(keywordPrefix, ((doc1, doc2) -> {
            if (preFixCount(doc1, newKeyword) > preFixCount(doc2, newKeyword)) {
                return -1;
            } else if (preFixCount(doc1, newKeyword) < preFixCount(doc2, newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        }));
        for(Document doc: sortedPreList){
            doc.setLastUseTime(nanoTime());
        }
        return sortedPreList;
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
            removeDocFromHeap(doc);
            this.docCounter -= 1;
            this.currentBytes -= this.getMemoryUsage(previous);
            Function<URI,Boolean> undo = (URI u) ->{
                this.table.put(u, previous);
                previous.setLastUseTime(nanoTime());
                this.heap.insert(previous);
                this.heap.reHeapify(previous);
                this.docCounter += 1;
                this.currentBytes += this.getMemoryUsage(previous);
                for(String word: previous.getWords()){
                    this.tree.put(word, previous);
                }
                while(isFull()){
                    Document doc2 = this.heap.remove();
                    removeDocFromDocumentStore(doc2);
                    removeCommand(doc2);
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
            removeDocFromHeap(doc);
            this.docCounter -= 1;
            this.currentBytes -= this.getMemoryUsage(previous);
            Function<URI,Boolean> undo = (URI u) ->{
                this.table.put(u, previous);
                previous.setLastUseTime(nanoTime());
                this.heap.insert(previous);
                this.heap.reHeapify(previous);
                this.docCounter += 1;
                this.currentBytes += this.getMemoryUsage(previous);
                for(String word: previous.getWords()){
                    this.tree.put(word, previous);
                }
                while(isFull()){
                    Document doc2 = this.heap.remove();
                    removeDocFromDocumentStore(doc2);
                    removeCommand(doc2);
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
     * set maximum number of documents that may be stored
     * @param limit
     */
    @Override
    public void setMaxDocumentCount(int limit) {
        this.maxDoc = limit;
        while(isFull()){
            Document doc2 = this.heap.remove();
            removeDocFromDocumentStore(doc2);
            removeCommand(doc2);
        }
    }

    /**
     * set maximum number of bytes of memory that may be used by all the documents in memory combined
     * @param limit
     */
    @Override
    public void setMaxDocumentBytes(int limit) {
        this.maxBinary = limit;
        while(isFull()){
            Document doc2 = this.heap.remove();
            removeDocFromDocumentStore(doc2);
            removeCommand(doc2);
        }
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

    private int getMemoryUsage(Document doc){
        String words = doc.getDocumentTxt();
        if(words != null){
            return words.getBytes().length;
        }else{
            return doc.getDocumentBinaryData().length;
        }
    }


    private boolean isFull(){
        if(this.maxDoc == 0 && this.maxBinary == 0){
            return false;
        }else if ((this.maxDoc > 0 && this.docCounter > maxDoc) || (this.maxBinary > 0 && currentBytes > this.maxBinary)){
            return true;
        }else{
            return false;
        }
    }

    private void removeDocFromHeap(Document doc){
        doc.setLastUseTime(Long.MIN_VALUE);
        this.heap.reHeapify(doc);
        this.heap.remove();
    }

    private void removeDocFromDocumentStore(Document doc){
        this.table.put(doc.getKey(),null);
        for(String word: doc.getWords()){
            tree.delete(word, doc);
        }
        docCounter -= 1;
        currentBytes -= this.getMemoryUsage(doc);
    }
}
