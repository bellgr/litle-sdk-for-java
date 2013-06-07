package com.litle.sdk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.litle.sdk.generate.AccountUpdate;
import com.litle.sdk.generate.AuthReversal;
import com.litle.sdk.generate.Authorization;
import com.litle.sdk.generate.BatchRequest;
import com.litle.sdk.generate.Capture;
import com.litle.sdk.generate.CaptureGivenAuth;
import com.litle.sdk.generate.Credit;
import com.litle.sdk.generate.EcheckCredit;
import com.litle.sdk.generate.EcheckRedeposit;
import com.litle.sdk.generate.EcheckSale;
import com.litle.sdk.generate.EcheckVerification;
import com.litle.sdk.generate.ForceCapture;
import com.litle.sdk.generate.ObjectFactory;
import com.litle.sdk.generate.RegisterTokenRequestType;
import com.litle.sdk.generate.Sale;
import com.litle.sdk.generate.TransactionType;
import com.litle.sdk.generate.UpdateCardValidationNumOnToken;

public class LitleBatchRequest {
    private BatchRequest batchRequest;
    private JAXBContext jc;
    private Properties config;
    private ObjectFactory objectFactory;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;
    private Communication communication;

    private File txnsFile;
    private File batchFile;
    private Integer totalTxns;
    private final static int MAX_TXNS_PER_BATCH = 100000;

    public LitleBatchRequest(){
        // must be in this order
        init();
    }

    public LitleBatchRequest(File batchFile, File txnsFile){
        // they passed us extant batch/txns files. let's help them out!
        if(batchFile.exists() && txnsFile.exists()){
            if(batchFile.getName().matches("batch_\\d+.closed-\\d+\\z")){ //unless those jerks sent us a closed batch
                init();
            }
            else{
                this.txnsFile = txnsFile;
                this.batchFile = batchFile;
                initUtils();
                //do unmarshalling here
                try {
                    batchRequest = (BatchRequest)unmarshaller.unmarshal(batchFile);
                } catch (JAXBException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                calculateTotalTxns();
            }
        }
        else{
            init(); //silly merchant brogrammer sent us nonexistant files
        }
    }

    private void init(){
        initUtils();
        initBatch();
        totalTxns = 0;
    }

    private void initBatch(){
        batchRequest = objectFactory.createBatchRequest();

        batchRequest.setAuthAmount(BigInteger.valueOf(0));
        batchRequest.setNumAuths(BigInteger.valueOf(0));

        batchRequest.setSaleAmount(BigInteger.valueOf(0));
        batchRequest.setNumSales(BigInteger.valueOf(0));

        batchRequest.setCreditAmount(BigInteger.valueOf(0));
        batchRequest.setNumCredits(BigInteger.valueOf(0));

        batchRequest.setNumTokenRegistrations(BigInteger.valueOf(0));

        batchRequest.setCaptureGivenAuthAmount(BigInteger.valueOf(0));
        batchRequest.setNumCaptureGivenAuths(BigInteger.valueOf(0));

        batchRequest.setForceCaptureAmount(BigInteger.valueOf(0));
        batchRequest.setNumForceCaptures(BigInteger.valueOf(0));

        batchRequest.setAuthReversalAmount(BigInteger.valueOf(0));
        batchRequest.setNumAuthReversals(BigInteger.valueOf(0));

        batchRequest.setCaptureAmount(BigInteger.valueOf(0));
        batchRequest.setNumCaptures(BigInteger.valueOf(0));

        batchRequest.setEcheckVerificationAmount(BigInteger.valueOf(0));
        batchRequest.setNumEcheckVerification(BigInteger.valueOf(0));

        batchRequest.setEcheckCreditAmount(BigInteger.valueOf(0));
        batchRequest.setNumEcheckCredit(BigInteger.valueOf(0));

        batchRequest.setNumEcheckRedeposit(BigInteger.valueOf(0));

        batchRequest.setEcheckSalesAmount(BigInteger.valueOf(0));
        batchRequest.setNumEcheckSales(BigInteger.valueOf(0));

        batchRequest.setNumUpdateCardValidationNumOnTokens(BigInteger.valueOf(0));

        batchRequest.setNumAccountUpdates(BigInteger.valueOf(0));

        try {
            batchFile = this.generateFile();
            txnsFile = new File(batchFile.getAbsolutePath() + "_txns");
            txnsFile.createNewFile();
        }
        catch(IOException e){
            e.printStackTrace();
        }

        System.out.println("Created file at: " + batchFile.getAbsoluteFile());
    }

    private void initUtils(){
        try{
            jc = JAXBContext.newInstance("com.litle.sdk.generate");
            marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = jc.createUnmarshaller();
            communication = new Communication();
            objectFactory = new ObjectFactory();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void calculateTotalTxns(){
        totalTxns = batchRequest.getNumSales().intValue() +
                batchRequest.getNumAuths().intValue() +
                batchRequest.getNumCredits().intValue() +
                batchRequest.getNumTokenRegistrations().intValue() +
                batchRequest.getNumCaptureGivenAuths().intValue() +
                batchRequest.getNumForceCaptures().intValue() +
                batchRequest.getNumAuthReversals().intValue() +
                batchRequest.getNumCaptures().intValue() +
                batchRequest.getNumEcheckVerification().intValue() +
                batchRequest.getNumEcheckCredit().intValue() +
                batchRequest.getNumEcheckRedeposit().intValue() +
                batchRequest.getNumEcheckSales().intValue() +
                batchRequest.getNumUpdateCardValidationNumOnTokens().intValue() +
                batchRequest.getNumAccountUpdates().intValue();
    }

    public void addTransaction(TransactionType txn) throws Exception{

        //oh no! they tried to put too many transactions in a batch. let's help a brother out
        if(totalTxns+1 > MAX_TXNS_PER_BATCH){
            this.closeBatch();
            //reinitialize the batch
            init();
        }


        StringWriter strw = new StringWriter();
        JAXBElement transactionType;

        if(txn instanceof Sale){
            batchRequest.setNumSales(batchRequest.getNumSales().add(BigInteger.valueOf(1)));
            batchRequest.setSaleAmount(batchRequest.getSaleAmount().add(BigInteger.valueOf(((Sale) txn).getAmount())));
            transactionType = objectFactory.createSale((Sale)txn);

        } else if (txn instanceof Authorization){
            batchRequest.setNumAuths(batchRequest.getNumAuths().add(BigInteger.valueOf(1)));
            batchRequest.setAuthAmount(batchRequest.getAuthAmount().add(BigInteger.valueOf(((Authorization) txn).getAmount())));
            transactionType = objectFactory.createAuthorization((Authorization)txn);

        } else if (txn instanceof Credit){
            batchRequest.setNumCredits(batchRequest.getNumCredits().add(BigInteger.valueOf(1)));
            batchRequest.setCreditAmount(batchRequest.getCreditAmount().add(BigInteger.valueOf(((Credit) txn).getAmount())));
            transactionType = objectFactory.createCredit((Credit)txn);

        } else if (txn instanceof RegisterTokenRequestType){
            batchRequest.setNumTokenRegistrations(batchRequest.getNumTokenRegistrations().add(BigInteger.valueOf(1)));
            transactionType = objectFactory.createRegisterTokenRequest((RegisterTokenRequestType)txn);

        } else if (txn instanceof CaptureGivenAuth){
            batchRequest.setNumCaptureGivenAuths(batchRequest.getNumCaptureGivenAuths().add(BigInteger.valueOf(1)));
            batchRequest.setCaptureGivenAuthAmount(batchRequest.getCaptureGivenAuthAmount().add(BigInteger.valueOf(((CaptureGivenAuth) txn).getAmount())));
            transactionType = objectFactory.createCaptureGivenAuth((CaptureGivenAuth)txn);

        } else if (txn instanceof ForceCapture){
            batchRequest.setNumForceCaptures(batchRequest.getNumForceCaptures().add(BigInteger.valueOf(1)));
            batchRequest.setForceCaptureAmount(batchRequest.getForceCaptureAmount().add(BigInteger.valueOf(((ForceCapture) txn).getAmount())));
            transactionType = objectFactory.createForceCapture((ForceCapture)txn);

        } else if (txn instanceof AuthReversal){
            batchRequest.setNumAuthReversals(batchRequest.getNumAuthReversals().add(BigInteger.valueOf(1)));
            batchRequest.setAuthReversalAmount(batchRequest.getAuthReversalAmount().add(BigInteger.valueOf(((AuthReversal) txn).getAmount())));
            transactionType = objectFactory.createAuthReversal((AuthReversal)txn);

        } else if (txn instanceof Capture){
            batchRequest.setNumCaptures(batchRequest.getNumCaptures().add(BigInteger.valueOf(1)));
            batchRequest.setCaptureAmount(batchRequest.getCaptureAmount().add(BigInteger.valueOf(((Capture) txn).getAmount())));
            transactionType = objectFactory.createCapture((Capture)txn);

        } else if (txn instanceof EcheckVerification){
            batchRequest.setNumEcheckVerification(batchRequest.getNumEcheckVerification().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckVerificationAmount(batchRequest.getEcheckVerificationAmount().add(BigInteger.valueOf(((EcheckVerification) txn).getAmount())));
            transactionType = objectFactory.createEcheckVerification((EcheckVerification)txn);

        } else if (txn instanceof EcheckCredit){
            batchRequest.setNumEcheckCredit(batchRequest.getNumEcheckCredit().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckCreditAmount(batchRequest.getEcheckCreditAmount().add(BigInteger.valueOf(((EcheckCredit) txn).getAmount())));
            transactionType = objectFactory.createEcheckCredit((EcheckCredit)txn);

        } else if (txn instanceof EcheckRedeposit){
            batchRequest.setNumEcheckRedeposit(batchRequest.getNumEcheckRedeposit().add(BigInteger.valueOf(1)));
            transactionType = objectFactory.createEcheckRedeposit((EcheckRedeposit)txn);

        } else if (txn instanceof EcheckSale){
            batchRequest.setNumEcheckSales(batchRequest.getNumEcheckSales().add(BigInteger.valueOf(1)));
            batchRequest.setEcheckSalesAmount(batchRequest.getEcheckSalesAmount().add(BigInteger.valueOf(((EcheckSale) txn).getAmount())));
            transactionType = objectFactory.createEcheckSale((EcheckSale)txn);

        } else if (txn instanceof UpdateCardValidationNumOnToken){
            batchRequest.setNumUpdateCardValidationNumOnTokens(batchRequest.getNumUpdateCardValidationNumOnTokens().add(BigInteger.valueOf(1)));
            transactionType = objectFactory.createUpdateCardValidationNumOnToken((UpdateCardValidationNumOnToken)txn);

        } else if (txn instanceof AccountUpdate){
            batchRequest.setNumAccountUpdates(batchRequest.getNumAccountUpdates().add(BigInteger.valueOf(1)));
            transactionType = objectFactory.createAccountUpdate((AccountUpdate)txn);
        } else{
            throw new Exception();
        }

        try {
            marshaller.marshal(transactionType, strw);
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        totalTxns++;

        String xml = strw.toString();
        xml = xml.replaceAll(" xmlns=\".*\"", "");
        FileWriter writer = new FileWriter(txnsFile, true);
        writer.write(xml);
        writer.write("\n");
        writer.close();

        //now marshal the newly updated batch object to the batch file
        try{
            marshaller.marshal(batchRequest, batchFile);
        } catch(JAXBException e){
            e.printStackTrace();
        }

    }

    //guarantees unique file generation
    private File generateFile() throws IOException{
        String filename = "batch_" + String.valueOf(System.currentTimeMillis());
        File file = new File(filename);
        if(file.exists()) {
            return generateFile();
        } else{
            file.createNewFile();
            return file;
        }
    }

    public void closeBatch() {
        BufferedReader inputStream = null;
        FileWriter outputStream = null;
        try{
            inputStream = new BufferedReader(new FileReader(txnsFile));
            outputStream = new FileWriter(batchFile);
            StringWriter strw = new StringWriter();

            try {
                marshaller.marshal(batchRequest, strw);
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String header = strw.toString();
            header = header.replaceAll(" xmlns=\".*\"", "");
            header = header.replaceAll("/>", ">");
            outputStream.write(header);
            outputStream.write("\n");

            String l;
            while((l = inputStream.readLine()) != null){
                outputStream.write(l);
                outputStream.write("\n");
            }
            outputStream.write("</batchRequest>");
            inputStream.close();
            outputStream.close();
            batchFile.renameTo(new File(batchFile.getAbsolutePath() + ".closed-" + totalTxns.toString()));
            txnsFile.delete();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }


}
