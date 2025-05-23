// ChatService.java (new interface)
package org.example.rmi;
import org.example.domain.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ChatService extends Remote {
//    void createChat(Chat chat) throws RemoteException;
    List<ChatGroup> getAllChats() throws RemoteException;
//    void subscribeUserToChat(int userId, int chatId) throws RemoteException;
//    void unsubscribeUserFromChat(int userId, int chatId) throws RemoteException;
//    void sendMessageToChat(int chatId, String message) throws RemoteException;
    void sendMessage(String message, User sender, int chatId) throws RemoteException;
    void subscribe(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException;
    void unsubscribe(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException;

    User getUserByUsername(String username) throws RemoteException;

    void createChat(ChatGroup chatGroup) throws RemoteException;
    void deleteChat(int chatId) throws RemoteException;
    List<User> getUsersInChat(int chatId) throws RemoteException;
    void removeUserFromChat(int userId, int chatId) throws RemoteException;
    void subscribeToChat(int userId, int chatId) throws RemoteException;
   // void unsubscribeFromChat(int userId, int chatId) throws RemoteException;

    void unsubscribeFromChat(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException;

    List<Message> getChatMessages(int chatId) throws RemoteException;
    void sendAdminMessage(String message, User sender, int chatId) throws RemoteException;
    List<ChatMessage> getAllChatMessages(int chatId) throws RemoteException;

    List<User> getAllUsers() throws RemoteException;



    void addUserToGroup(int userId, int groupId) throws RemoteException;
}