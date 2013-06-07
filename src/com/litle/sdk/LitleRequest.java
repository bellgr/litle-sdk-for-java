package com.litle.sdk;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.litle.sdk.generate.BatchRequest;
import com.litle.sdk.generate.ObjectFactory;

public class LitleRequest {

    private BatchRequest batchRequest;
    private JAXBContext jc;
    private Properties config;
    private ObjectFactory objectFactory;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;
    private Communication communication;
    com.litle.sdk.generate.LitleRequest litleRequest = new com.litle.sdk.generate.LitleRequest();

    private File batchesFile;
    private File requestFile;

    private Integer totalTxns;

    private static final int MAX_TXNS_IN_REQUEST = 500000;


    public LitleRequest(){
        init();
    }

    private void init(){
        totalTxns = 0;
        try{

            jc = JAXBContext.newInstance("com.litle.sdk.generate");
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = jc.createUnmarshaller();
            communication = new Communication();
            objectFactory = new ObjectFactory();
            batchRequest = objectFactory.createBatchRequest();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        try {
            requestFile = this.generateFile();
            batchesFile = new File(requestFile.getAbsolutePath() + "_batches");
            batchesFile.createNewFile();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Created file at: " + requestFile.getAbsoluteFile());
    }

    public void addBatch(File batchFile){
        if(batchFile.exists() && batchFile.getName().matches("batch_\\d+.closed-\\d+\\z")){
            String fname = batchFile.getName();
            fname = fname.replaceAll("batch_\\d+.closed-", "");
            Integer numTxns = Integer.parseInt(fname);
            if((totalTxns + numTxns) > MAX_TXNS_IN_REQUEST){
                throw
            }
            else{

            }


        }
        else {
            //dumdum
        }
    }

    public void addBatch(LitleBatchRequest batch){

    }


    public void closeRequest(){

    }
    //guarantees unique file generation
    private File generateFile() throws IOException{
        String filename = "request_" + String.valueOf(System.currentTimeMillis());
        File file = new File(filename);
        if(file.exists()) {
            return generateFile();
        } else{
            file.createNewFile();
            return file;
        }
    }


}
