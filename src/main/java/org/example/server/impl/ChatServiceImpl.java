package org.example.server.impl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.domain.*;
import org.example.rmi.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChatServiceImpl extends UnicastRemoteObject implements ChatService {
    private List<ChatObserver> observers = new ArrayList<>();
    private final SessionFactory sessionFactory;
    public ChatServiceImpl(SessionFactory sessionFactory) throws RemoteException {
        this.sessionFactory = sessionFactory;
        this.observers = new ArrayList<>();
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public User getUserByUsername(String username) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        } catch (Exception e) {
            throw new RemoteException("Error finding user", e);
        }
    }

    // ChatServiceImpl.java
    @Override
    public void createChat(ChatGroup chat) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(chat);
            tx.commit();
            System.out.println("Chat created: " + chat.getChatName()); // Log success
        } catch (Exception e) {
            System.err.println("Error creating chat: " + e.getMessage()); // Log error
            throw new RemoteException("Error creating chat", e);
        }
    }

    @Override
    public void deleteChat(int chatId) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            // Delete the chat group
            ChatGroup chatGroup = session.get(ChatGroup.class, chatId);
            if (chatGroup != null) {
                session.delete(chatGroup);
            }

            transaction.commit();
        } catch (Exception e) {
            throw new RemoteException("Error deleting chat", e);
        }
    }

    @Override
    public List<User> getUsersInChat(int chatId) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            ChatGroup group = session.get(ChatGroup.class, chatId);
            return new ArrayList<>(group.getParticipants());
        } catch (Exception e) {
            throw new RemoteException("Error fetching users", e);
        }
    }

    @Override
    public void removeUserFromChat(int userId, int chatId) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            Transaction tx = session.beginTransaction();

            ChatGroup group = session.get(ChatGroup.class, chatId);
            User user = session.get(User.class, userId);

            if (group != null && user != null) {
                group.getParticipants().remove(user);
                session.update(group);
            }

            tx.commit();
        } catch (Exception e) {
            throw new RemoteException("Error removing user", e);
        }
    }

    @Override
    public void subscribeToChat(int userId, int chatId) throws RemoteException {

    }

    @Override
    public void unsubscribeFromChat(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException {
        try {
            // Add null checks
            if (observer == null) {
                System.err.println("Attempted to unsubscribe null observer");
                return;
            }

            if (observers == null) {
                System.err.println("Observers list not initialized");
                return;
            }

            // Remove observer safely
            if (observers.remove(observer)) {
                System.out.println("Unsubscribed user " + user.getUsername() + " from chat " + chatId);

                // Additional cleanup logic
                if (chatLog != null) {
                    chatLog.setEnd_time(LocalDateTime.now());

                    // Get the formatted time
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
                    String formattedTime = chatLog.getEnd_time().format(formatter);

                    // Notify all observers that user has left
                    String leftMessage = user.getNickname() + " left: " + formattedTime;
                    notifyAllObservers(leftMessage, chatId);

                    // Check if this was the last user in the chat
                    checkLastUserAndSaveChat(chatId, chatLog);
                }
            } else {
                System.err.println("Observer not found in subscribers list");
            }
        } catch (Exception e) {
            throw new RemoteException("Unsubscribe failed", e);
        }
    }

    @Override
    public List<Message> getChatMessages(int chatId) throws RemoteException {
        return List.of();
    }


    @Override
    public List<ChatMessage> getAllChatMessages(int chatId) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT m FROM ChatMessage m WHERE m.chatGroup.chatId = :chatId ORDER BY m.start_at ASC",
                            ChatMessage.class
                    )
                    .setParameter("chatId", chatId)
                    .list();
        } catch (Exception e) {
            throw new RemoteException("Error fetching chat messages", e);
        }
    }


    @Override
    public List<ChatGroup> getAllChats() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                    "SELECT c FROM ChatGroup c LEFT JOIN FETCH c.admin",
                    ChatGroup.class
            ).list();
        } catch (Exception e) {
            throw new RemoteException("Error fetching chats", e);
        }
    }

    @Override
    public void sendMessage(String message, User sender, int chatId) throws RemoteException {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            System.out.println("test 1");
            tx = session.beginTransaction();

            // Load existing ChatGroup from DB
            ChatGroup chatGroup = session.get(ChatGroup.class, chatId);
            System.out.println("test 2");
            if (chatGroup == null) {
                throw new IllegalArgumentException("Chat group not found with id: " + chatId);
            }

            System.out.println("test 3");
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setUser(sender); // sender must also be attached/managed
            chatMessage.setChatGroup(chatGroup);
            chatMessage.setMessage(sender.getNickname() + ": " + message);
            chatMessage.setStart_at(LocalDateTime.now());

            session.persist(chatMessage);

            tx.commit();
            System.out.println("Message sent and saved to DB");

            notifyAllObservers(sender.getNickname() + ": " + message, chatId);
        } catch (Exception e) {
            if (tx != null && tx.getStatus().canRollback()) {
                tx.rollback();
            }
            throw new RemoteException("Error sending message", e);
        }
    }


    @Override
    public void subscribe(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException {
        observers.add(observer);

        // Get the formatted time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String formattedTime = chatLog.getStart_time().format(formatter);
        sendMessage(user.getNickname() + " joined: " + formattedTime, user, chatId);
        notifyAllObservers(user.getNickname() + " joined: "+ formattedTime, chatId); // Send message to all observers
    }

    @Override
    public void unsubscribe(User user, ChatObserver observer, ChatLog chatLog, int chatId) throws RemoteException {
        observers.remove(observer);

        // Get the formatted time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String formattedTime = chatLog.getEnd_time().format(formatter);
        sendMessage(user.getNickname() + " left: " + formattedTime, user, chatId);
        notifyAllObservers(user.getNickname() + " left: " + formattedTime, chatId);

        // Check if this was the last user in the chat
        checkLastUserAndSaveChat(chatId, chatLog);
    }

    private void checkLastUserAndSaveChat(int chatId, ChatLog chatLog) throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            // Count active users in this chat (users with ChatLog entries where end_time is NULL)
            Long activeUsersCount = session.createQuery(
                    "SELECT COUNT(c) FROM ChatLog c WHERE c.chat_id = :chatId AND c.end_time IS NULL",
                    Long.class
                )
                .setParameter("chatId", chatId)
                .uniqueResult();

            if (activeUsersCount != null && activeUsersCount == 0) {
                // This was the last user, save chat history and update log
                LocalDateTime endTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
                String formattedTime = endTime.format(formatter);

                // Notify that chat has stopped
                String stopMessage = "Chat stopped at: " + formattedTime;
                notifyAllObservers(stopMessage, chatId);

                // Save chat history to file
                String filePath = saveChatHistoryToFile(chatId);

                // Update the ChatLog with the file path
                if (chatLog != null && filePath != null) {
                    Transaction tx = session.beginTransaction();
                    chatLog.setChatFilePath(filePath);
                    session.update(chatLog);
                    tx.commit();
                }
            }
        } catch (Exception e) {
            throw new RemoteException("Error checking last user in chat", e);
        }
    }

    private String saveChatHistoryToFile(int chatId) throws RemoteException {
        try {
            // Get all messages for this chat
            List<ChatMessage> messages = getAllChatMessages(chatId);
            if (messages.isEmpty()) {
                return null;
            }

            // Create directory for chat logs if it doesn't exist
            java.io.File chatLogsDir = new java.io.File("chat_logs");
            if (!chatLogsDir.exists()) {
                chatLogsDir.mkdir();
            }

            // Create file name with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "chat_" + chatId + "_" + timestamp + ".txt";
            java.io.File chatFile = new java.io.File(chatLogsDir, fileName);

            // Write messages to file
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(chatFile))) {
                writer.println("Chat ID: " + chatId);
                writer.println("Saved at: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("----------------------------------------");

                for (ChatMessage message : messages) {
                    String formattedTime = message.getStart_at().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    writer.println("[" + formattedTime + "] " + message.getMessage());
                }
            }

            System.out.println("Chat history saved to: " + chatFile.getAbsolutePath());
            return chatFile.getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Error saving chat history: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<User> getAllUsers() throws RemoteException {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User", User.class).list();
        } catch (Exception e) {
            throw new RemoteException("Error fetching users", e);
        }
    }

    @Override
    public void addUserToGroup(int userId, int groupId) throws RemoteException {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            User user = session.get(User.class, userId);
            ChatGroup chatGroup = session.get(ChatGroup.class, groupId);

            if (user == null || chatGroup == null) {
                throw new RemoteException("User or Chat Group not found");
            }

            ChatUserId id = new ChatUserId(userId, groupId);

            // Avoid duplicate entry
            ChatUser existing = session.get(ChatUser.class, id);
            if (existing != null) {
                throw new RemoteException("User is already in the group");
            }

            ChatUser chatUser = new ChatUser();
            chatUser.setId(id);
            chatUser.setUser(user);
            chatUser.setChatGroup(chatGroup);

            session.persist(chatUser);
            tx.commit();

            System.out.println("User added to group successfully");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new RemoteException("Error adding user to group", e);
        }
    }

    @Override
    public void sendAdminMessage(String message, User adminUser, int groupId) throws RemoteException {
        Transaction tx = null;
        try (Session session = sessionFactory.openSession()) {
            tx = session.beginTransaction();

            // Verify admin user exists
            User managedAdmin = session.get(User.class, adminUser.getUser_id());
            if (managedAdmin == null) {
                throw new RemoteException("Admin user not found");
            }

            // Load chat group with admin relationship
            ChatGroup chatGroup = session.createQuery(
                    "FROM ChatGroup g LEFT JOIN FETCH g.admin WHERE g.chatId = :groupId",
                    ChatGroup.class
            ).setParameter("groupId", groupId).uniqueResult();

            if (chatGroup == null) {
                throw new RemoteException("Chat group not found");
            }

            // Validate admin privileges
//            if (!chatGroup.getAdmin().getUser_id().equals(managedAdmin.getUser_id())) {
//                throw new RemoteException("User lacks admin privileges");
//            }

            // Create and persist message
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setUser(managedAdmin);
            chatMessage.setChatGroup(chatGroup);
            chatMessage.setMessage(message);
            chatMessage.setStart_at(LocalDateTime.now());

            session.persist(chatMessage);
            tx.commit();

            // Notify observers
            notifyAllObservers(message, groupId);

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw new RemoteException("Failed to send admin message: " + e.getMessage(), e);
        }
    }

    private void notifyAllObservers(String message, int chatId) {
        observers.forEach(obs -> {
            try { obs.notifyNewMessage(message, chatId); }
            catch (RemoteException e) { e.printStackTrace(); }
        });

    }



}
