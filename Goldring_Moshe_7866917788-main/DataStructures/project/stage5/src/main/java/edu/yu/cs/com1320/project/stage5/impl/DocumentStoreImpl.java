package edu.yu.cs.com1320.project.stage5.impl;


import com.sun.jdi.ObjectReference;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

import static java.lang.System.nanoTime;

public class DocumentStoreImpl implements DocumentStore {
    private StackImpl<Undoable> commandStack;
    private TrieImpl<URI> tree;
    private MinHeapImpl<URI> heap;
    private BTreeImpl<URI, Document> bTree;
    private HashSet<URI> docsOnDisk = new HashSet<>();
    private int maxDoc = 0;
    private int maxBinary = 0;
    private int docCounter = 0;
    private int currentBytes = 0;

    public DocumentStoreImpl(File file){
        this.commandStack = new StackImpl<>();
        this.tree = new TrieImpl<>();
        this.bTree = new BTreeImpl<>();
        this.heap = new MinHeapImpl<>(this.bTree);
        this.bTree.setPersistenceManager(new DocumentPersistenceManager(file));
    }

    public DocumentStoreImpl(){
        this.commandStack = new StackImpl<>();
        this.tree = new TrieImpl<>();
        this.bTree = new BTreeImpl<>();
        this.heap = new MinHeapImpl<>(this.bTree);
        this.bTree.setPersistenceManager(new DocumentPersistenceManager(null));
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
            document = new DocumentImpl(uri, inputTxt, null);
        }else{
            byte [] inputByte = input.readAllBytes();
            document = new DocumentImpl(uri, inputByte);
        }
        if(this.bTreeGet(uri) == null){
            this.bTree.put(uri, document);
            this.heap.insert(uri);
            document.setLastUseTime(nanoTime());
            this.heap.reHeapify(uri);
            this.docCounter += 1;
            this.currentBytes += this.getMemoryUsage(uri);
            TriePut(uri);
            while(isFull()){
                URI uri1 = this.heap.remove();
                removeDocFromDocumentStore(uri1);
            }
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc = getDocument(uri);
                for(String word: doc.getWords()){
                    tree.delete(word, uri);
                }
                removeDocFromHeap(uri);
                this.bTree.put((URI)u, null);
                this.docCounter -= 1;
                return true;
            }));
            return 0;
        }else {
            Document doc = getDocument(uri);
            this.currentBytes -= this.getMemoryUsage(uri);
            for(String word: doc.getWords()){
                tree.delete(word, uri);
            }
            Document previous = this.bTree.put(uri, document);
            this.heap.insert(uri);
            document.setLastUseTime(nanoTime());
            this.heap.reHeapify(uri);
            TriePut(uri);
            this.currentBytes += this.getMemoryUsage(document.getKey());
            while(isFull()){
                URI uri1 = this.heap.remove();
                removeDocFromDocumentStore(uri1);
            }
            commandStack.push(new GenericCommand(uri, (u) -> {
                Document doc2 = getDocument(uri);
                for(String word: doc2.getWords()){
                    tree.delete(word, uri);
                }
                this.removeDocFromHeap(uri);
                this.currentBytes -= this.getMemoryUsage(uri);
                this.bTree.put((URI)u, previous);
                this.heap.insert(uri);
                previous.setLastUseTime(nanoTime());
                this.heap.reHeapify(uri);
                TriePut(uri);
                this.currentBytes += this.getMemoryUsage(uri);
                while(isFull()){
                    URI uri2 = this.heap.remove();
                    removeDocFromDocumentStore(uri2);
                }
                return true;
            }));
            return previous.hashCode();
        }
    }

    private int NullInputPut(InputStream input, URI uri, DocumentFormat format) throws IOException{
        if(this.bTreeGet(uri) == null){
            this.bTree.put(uri, null);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.bTree.put((URI)u, null);
                return true;
            }));
            return 0;
        }else{
            //essentially a delete
            Document doc = getDocument(uri);
            for(String word: doc.getWords()){
                tree.delete(word, uri);
            }
            this.currentBytes -= this.getMemoryUsage(uri);
            Document previous = this.bTree.put(uri, null);
            removeDocFromHeap(uri);
            this.docCounter -= 1;
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.bTree.put((URI)u, previous);
                previous.setLastUseTime(nanoTime());
                this.TriePut((URI) u);
                this.heap.insert((URI) u);
                this.heap.reHeapify((URI) u);
                this.currentBytes += this.getMemoryUsage((URI) u);
                this.docCounter += 1;
                while(isFull()){
                    URI uri1 = this.heap.remove();
                    removeDocFromDocumentStore(uri1);
                }
                return true;
            }));
            return previous.hashCode();
        }
    }

    private void TriePut(URI previous){
        URI uri = previous;
        Document doc = bTreeGet(uri);
        for(String word: doc.getWords()){
            tree.put(word, uri);
        }
    }
    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document getDocument(URI uri) {
        Document doc = this.bTreeGet(uri);
        return doc;
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if(this.bTreeGet(uri) != null){
            Document doc = getDocument(uri);
            for(String word: doc.getWords()){
                tree.delete(word, uri);
            }
            this.currentBytes -= getMemoryUsage(uri);
            removeDocFromHeap(uri);
            Document previous = this.bTree.put(uri, null);
            this.docCounter -= 1;
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.bTree.put((URI)u, previous);
                this.heap.insert((URI)u);
                previous.setLastUseTime(nanoTime());
                this.heap.reHeapify((URI)u);
                TriePut((URI)u);
                this.docCounter += 1;
                this.currentBytes += getMemoryUsage((URI)u);
                while(isFull()){
                    URI uri2 = this.heap.remove();
                    removeDocFromDocumentStore(uri2);
                }
                return true;
            }));
            return true;
        }else{
            Document current = this.bTreeGet(uri);
            commandStack.push(new GenericCommand(uri, (u) -> {
                this.bTree.put((URI)u, current);
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
        List<URI> sortedList = this.tree.getAllSorted(keyword, (doc1, doc2) -> {
            if (bTreeGet(doc1).wordCount(newKeyword) > bTreeGet(doc2).wordCount(newKeyword)) {
                return -1;
            } else if (bTreeGet(doc1).wordCount(newKeyword) < bTreeGet(doc2).wordCount(newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        });
        List<Document> returnList = new ArrayList<>();
        for(URI uri: sortedList){
            Document doc = bTreeGet(uri);
            returnList.add(doc);
            while(isFull()){
                URI uri2 = this.heap.remove();
                removeDocFromDocumentStore(uri2);
                returnList.remove(bTree.get(uri2));
            }
        }
        return returnList;
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
        List<URI> sortedPreList = this.tree.getAllWithPrefixSorted(keywordPrefix, ((doc1, doc2) -> {
            if (preFixCount(bTreeGet(doc1), newKeyword) > preFixCount(bTreeGet(doc2), newKeyword)) {
                return -1;
            } else if (preFixCount(bTreeGet(doc1), newKeyword) < preFixCount(bTreeGet(doc2), newKeyword)) {
                return 1;
            } else {
                return 0;
            }
        }));
        List<Document> returnList = new ArrayList<>();
        for(URI uri: sortedPreList){
            Document doc = bTreeGet(uri);
            returnList.add(doc);
            while(isFull()){
                URI uri2 = this.heap.remove();
                removeDocFromDocumentStore(uri2);
                returnList.remove(bTree.get(uri2));
            }
        }
        return returnList;
    }

    /**
     * Completely remove any trace of any document which contains the given keyword
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        Set<URI> deletions = tree.deleteAll(keyword);
        Set<URI> returnSet = new HashSet<>();
        for(URI uri: deletions){
            if(getDocument(uri) != null){
                returnSet.add(uri);
            }
        }
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        for(URI uri: deletions){
            this.currentBytes -= this.getMemoryUsage(uri);
            this.docCounter -= 1;
            removeDocFromHeap(uri);
            Document previous = this.bTree.put(uri, null);
            Function<URI,Boolean> undo = (URI u) ->{
                this.bTree.put(u, previous);
                previous.setLastUseTime(nanoTime());
                this.heap.insert(u);
                this.heap.reHeapify(u);
                this.docCounter += 1;
                this.currentBytes += this.getMemoryUsage(u);
                for(String word: previous.getWords()){
                    this.tree.put(word, u);
                }
                while(isFull()){
                    URI uri1 = this.heap.remove();
                    removeDocFromDocumentStore(uri1);
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
        Set<URI> deletions = tree.deleteAllWithPrefix(keywordPrefix);
        Set<URI> returnSet = new HashSet<>();
        for(URI uri: deletions){
            returnSet.add(bTreeGet(uri).getKey());
        }
        CommandSet<GenericCommand> commandSet = new CommandSet<>();
        for(URI deletedUri: deletions){
            removeDocFromHeap(deletedUri);
            this.docCounter -= 1;
            this.currentBytes -= this.getMemoryUsage(deletedUri);
            Document previous = this.bTree.put(deletedUri, null);
            Function<URI,Boolean> undo = (URI u) ->{
                this.bTree.put(u, previous);
                previous.setLastUseTime(nanoTime());
                this.heap.insert(u);
                this.heap.reHeapify(u);
                this.docCounter += 1;
                this.currentBytes += this.getMemoryUsage(u);
                for(String word: previous.getWords()){
                    this.tree.put(word, u);
                }
                while(isFull()){
                    URI uri1 = this.heap.remove();
                    removeDocFromDocumentStore(uri1);
                }
                return true;
            };
            GenericCommand command = new GenericCommand<>(deletedUri, undo);
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
            URI u = this.heap.remove();
            removeDocFromDocumentStore(u);
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
            URI u = this.heap.remove();
            removeDocFromDocumentStore(u);
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

    private int getMemoryUsage(URI uri){
        Document doc = bTree.get(uri);
        String words = doc.getDocumentTxt();
        doc.setLastUseTime(nanoTime());
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

    private void removeDocFromHeap(URI uri){
        bTreeGet(uri).setLastUseTime(Long.MIN_VALUE);
        this.heap.reHeapify(uri);
        this.heap.remove();
    }

    private void removeDocFromDocumentStore(URI uri){
        docCounter -= 1;
        docsOnDisk.add(uri);
        currentBytes -= this.getMemoryUsage(uri);
        try{
            bTree.moveToDisk(uri);
        }catch(Exception e){}
    }

    private Document bTreeGet(URI uri){
       if(docsOnDisk.contains(uri)){
           docsOnDisk.remove(uri);
           Document doc = bTree.get(uri);
           this.heap.insert(uri);
           doc.setLastUseTime(nanoTime());
           this.heap.reHeapify(uri);
           docCounter += 1;
           currentBytes += this.getMemoryUsage(uri);
           while (isFull()){
               URI uri2 = this.heap.remove();
               removeDocFromDocumentStore(uri2);
           }
           return doc;
        }else{
           Document doc = bTree.get(uri);
           if(doc!= null){
               doc.setLastUseTime(nanoTime());
           }
           return doc;
       }
    }
}
