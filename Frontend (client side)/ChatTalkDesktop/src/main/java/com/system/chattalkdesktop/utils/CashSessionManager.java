package com.system.chattalkdesktop.utils;

import java.util.HashMap;

public class CashSessionManager {

    private static CashSessionManager instance;
    private final HashMap<String, Long> chatCash;

    private CashSessionManager( ) {
        this.chatCash = new HashMap<>();
    }

    public static CashSessionManager getInstance() {
        if (instance == null) {
            instance = new CashSessionManager();
        }
        return instance;
    }

    public void addChatIdCash(String email,Long id){
        chatCash.put(email,id);
    }
    public Long getChatIdCash(String email){
        return chatCash.get(email);
    }

}
