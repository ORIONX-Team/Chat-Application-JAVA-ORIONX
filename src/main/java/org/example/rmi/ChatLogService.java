package org.example.rmi;

import org.example.domain.ChatLog;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ChatLogService extends Remote {
    ChatLog login(int user_id) throws RemoteException;
    ChatLog logout(int user_id) throws RemoteException;
    Boolean isUserOnline(int user_id) throws RemoteException;
}

