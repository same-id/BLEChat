package com.sam.blechat;

public class BLEServer {

    interface PutMessageCallback {

        void putMessageSuccess(String identifier);

        void putMessageError(Exception error);

    }

    interface getMessageCallback {

        void getMessageSuccess(String message);

        void getMessageError(Exception error);

    }

    interface ServerOperation {

        void cancel();
        
    }

    public void putMessage(String message) {

    }

    public void getMessage(String identifier) {

    }

}
