package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.impl.DocumentImpl;

import edu.yu.cs.com1320.project.stage5.PersistenceManager;

import java.io.File;
import java.io.IOException;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;

import static java.lang.System.nanoTime;

/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {
    private File baseDir;

    public DocumentPersistenceManager(File baseDir){
        if(baseDir == null){
            this.baseDir = new File(System.getProperty("user.dir"));
        }
        this.baseDir = baseDir;
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
        Gson gson = new Gson();
        String filePath = filePathGenerator(uri);
        File file = new File(this.baseDir, filePath);
        file.getParentFile().mkdirs();
        Writer writer = new FileWriter(file);
        writer.write(gson.toJson(val));
        writer.close();
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        Gson gson = new Gson();
        String filePath = filePathGenerator(uri);
        Document document;
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(this.baseDir, filePath)));
            document = gson.fromJson(bufferedReader, DocumentImpl.class);
            document.setLastUseTime(nanoTime());
        }catch(FileNotFoundException e){
            return null;
        }
        delete(uri);
        return document;

    }

    @Override
    public boolean delete(URI uri) throws IOException {
        String filePath = filePathGenerator(uri);
        File deletedFile = new File(this.baseDir, filePath);
        return deletedFile.delete();
    }

    private String filePathGenerator(URI uri){
        String newUri = uri.toString().replace("http://","");
        String filePath = newUri + ".json";
        return filePath;
    }

    private class DocumentSerializer implements JsonSerializer<Document>{

        @Override
        public JsonElement serialize(Document document, Type type, JsonSerializationContext jsonSerializationContext) {
            return null;
        }
    }

    private class DocumentDeserializer implements JsonDeserializer<Document>{

        @Override
        public Document deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            return null;
        }
    }
}
