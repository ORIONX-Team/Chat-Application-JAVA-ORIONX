package org.example.rmi;
import org.example.domain.ChatGroup;
import org.example.domain.User;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UserService extends Remote {
    User getUserByUsername(String username) throws RemoteException;
    List<User> getAllUsers() throws RemoteException;  // Add this
    void deleteUser(int userId) throws RemoteException;

    User getUser(int id) throws RemoteException;
    User checkEmailAndPassword(String email, String password) throws RemoteException;

    User registerUser(User user) throws RemoteException;


    void updateUser(User user) throws RemoteException;
    //List<String> getGroupDataByUserId(int userId) throws RemoteException;
    List<ChatGroup> getGroupDataByUserId(int userId) throws RemoteException;
}



