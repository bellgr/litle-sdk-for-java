package com.litle.transactions;

import com.litle.sdk.LitleBatchRequest;
import com.litle.sdk.generate.Sale;


public class Test {

    /**
     * @param args
     */



    public static void main(String[] args) {

        LitleBatchRequest request = new LitleBatchRequest();
        Long start = System.currentTimeMillis();
        for(int i = 0; i < 10; i++){
            Sale sale = new Sale();
            sale.setAmount(2312L);
            sale.setCustomerId("hurddurrrr");

            try {
                request.addTransaction(sale);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        request.closeBatch();
        System.out.println("Took: " + (System.currentTimeMillis() - start) + "ms");
    }

}
