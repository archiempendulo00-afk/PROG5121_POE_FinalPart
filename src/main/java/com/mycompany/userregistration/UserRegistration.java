/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.userregistration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author archi
 */
public class UserRegistration {

    public static void main(String[] args) {
        Login register = new Login();
        Scanner scan = new Scanner(System.in);
        MessageManager messages;
        try {
            messages = new MessageManager(Path.of("messages.json"));
        } catch (IOException exception) {
            System.out.println("Unable to load stored messages: " + exception.getMessage());
            scan.close();
            return;
        }
        
        System.out.println("\n=====Register=====");

        System.out.println("Please enter your first name: ");
        String firstName = scan.nextLine();

        System.out.println("Please enter your last name: ");
        String lastName = scan.nextLine();
        
        System.out.println("Please enter the username, useranme should contain 5 characters and an underscore: ");
        String name = scan.nextLine();
        
        System.out.println("Enter password, password should be 8 characters long, number/s and a speacial character.");
        String password = scan.nextLine();
        
        System.out.println("Enter cellphone number");
        String cellphone = scan.nextLine();
        
        while(true){
            if (!register.CheckUserName(name)){
                System.out.println("Please try again.");
                System.out.println("Please enter the username containing 5 characters and an underscore: ");
                name = scan.nextLine();
                continue; // Retry username validation.
            }
            if (!register.checkPasswordComplexity(password)){
                System.out.println("Please try again.");
                System.out.println("Enter password. password should be 8 characters long, number/s and a speacial character.");
                password = scan.nextLine();
                continue; // Retry password validation
            }
            if (!register.checkCellPhoneNumber(cellphone)){
                System.out.println("Please try again.");
                System.out.println("Enter cellphone number, +27XX XXX XXXX");
                cellphone = scan.nextLine();
                continue; // Retry cellphone number validation
            }
            System.out.println(register.registrationUser(firstName, lastName, name,
                password, cellphone));
            break; // Exit the loop after successful registration.
        }

        // Ask the user to log in.
        System.out.println("\n=====Login=====");
        System.out.println("Please enter your username: ");
        String loginName = scan.nextLine();
        System.out.println("Please enter your password: ");
        String loginPassword = scan.nextLine();

        boolean loggedIn = register.LoginUser(loginName, loginPassword);
        System.out.println(register.returnLoginStatus(loggedIn));

        if (!loggedIn) {
            scan.close();
            return;
        }

        // Show the menu for the app only after login works.
        System.out.println("Welcome to QuickChat.");
        while (true) {
            System.out.println("1) Send Messages");
            System.out.println("2) Show recently sent messages");
            System.out.println("3) Quit");
            System.out.println("4) Stored Messages");
            System.out.println("Please select an option (1, 2, 3, or 4):");
           String option = scan.nextLine();

            switch (option) {
                case "1":
                    handleSendMessages(scan, messages);
                    break;
                case "2":
                    displayMessages(messages.getSentMessages());
                    break;
                case "3":
                    System.out.println("Goodbye!");
                    scan.close();
                    return;
                case "4":
                    handleStoredMessages(scan, messages);
                    break;
                default:
                    System.out.println("Invalid option. Please choose 1, 2, 3, or 4.");
                    break;
            }
        }
    }

    private static void handleSendMessages(Scanner scan, MessageManager messages) {
        int numberOfMessages;
        while (true) {
            System.out.print("How many messages would you like to send? ");
            try {
                numberOfMessages = Integer.parseInt(scan.nextLine().trim());
                if (numberOfMessages >= 0) {
                    break;
                }
            } catch (NumberFormatException ignored) {
                // Prompt again for invalid input.
            }
            System.out.println("Please enter a non-negative whole number.");
        }

        for (int i = 0; i < numberOfMessages; i++) {
            System.out.println("Enter recipient number (Cellphone number):");
            String recipient = scan.nextLine();
            System.out.println("Enter message text:");
            String messageText = scan.nextLine();

            Message message = new Message(recipient, messageText);
            String recipientStatus = message.checkRecipientCell(recipient);
            String messageStatus = message.checkMessageLength(messageText);
            System.out.println(recipientStatus);
            System.out.println(messageStatus);

            if (!recipientStatus.equals("Cell phone number successfully captured.")
                    || messageStatus.startsWith("Message exceeds")) {
                continue;
            }

            System.out.println("1) Send Message");
            System.out.println("2) Disregard Message");
            System.out.println("3) Store Message");
            System.out.print("Choose an option: ");
            String selection = scan.nextLine().trim();
            String actionResult = message.SentMessage(selection);

            if (selection.equals("1") || selection.equalsIgnoreCase("send")
                    || selection.equalsIgnoreCase("send message")) {
                System.out.println(actionResult);
                message.setMessageHash(message.createMessageHash(
                        message.getMessageID(), message.getMessageNumber(), messageText));
                System.out.println("Message ID: " + message.getMessageID());
                System.out.println("Message Hash: " + message.getMessageHash());
                System.out.println("Recipient: " + message.getRecipient());
                System.out.println("Message: " + message.getMessageText());
                System.out.println("Total messages sent: " + Message.returnTotalMessages());
                messages.addSentMessage(message);
            } else if (selection.equals("2") || selection.equals("0")
                    || selection.equalsIgnoreCase("disregard")
                    || selection.equalsIgnoreCase("disregard message")) {
                System.out.println(actionResult);
                if (scan.nextLine().trim().equals("0")) {
                    System.out.println("Message deleted.");
                    messages.addDisregardedMessage(message);
                }
            } else if (selection.equals("3") || selection.equalsIgnoreCase("store")
                    || selection.equalsIgnoreCase("store message")) {
                try {
                    messages.storeMessage(message);
                    System.out.println("Message successfully stored.");
                } catch (IOException exception) {
                    System.out.println("Unable to store message: " + exception.getMessage());
                }
            } else {
                System.out.println(actionResult);
            }
        }
    }

    private static void displayMessages(List<Message> messages) {
        if (messages.isEmpty()) {
            System.out.println("No messages to display.");
            return;
        }
        for (Message message : messages) {
            System.out.println("Message ID: " + message.getMessageID());
            System.out.println("Message Hash: " + message.getMessageHash());
            System.out.println("Recipient: " + message.getRecipient());
            System.out.println("Message: " + message.getMessageText());
        }
    }

    private static void handleStoredMessages(Scanner scan, MessageManager messages) {
        while (true) {
            System.out.println("1) Display sender and recipient");
            System.out.println("2) Display longest stored message");
            System.out.println("3) Search by message ID");
            System.out.println("4) Search by recipient");
            System.out.println("5) Delete by message hash");
            System.out.println("6) Display stored message report");
            System.out.println("0) Return to main menu");
            System.out.print("Choose an option: ");
            String option = scan.nextLine().trim();

            switch (option) {
                case "1" -> {
                    for (Message message : messages.getStoredMessages()) {
                        System.out.println("Sender: Developer");
                        System.out.println("Recipient: " + message.getRecipient());
                    }
                }
                case "2" -> messages.getLongestStoredMessage().ifPresentOrElse(
                        message -> System.out.println("Longest message: " + message.getMessageText()),
                        () -> System.out.println("No stored messages."));
                case "3" -> {
                    System.out.print("Enter message ID: ");
                    String id = scan.nextLine().trim();
                    messages.findStoredMessageById(id).ifPresentOrElse(
                            message -> {
                                System.out.println("Recipient: " + message.getRecipient());
                                System.out.println("Message: " + message.getMessageText());
                            },
                            () -> System.out.println("Message ID not found."));
                }
                case "4" -> {
                    System.out.print("Enter recipient: ");
                    String recipient = scan.nextLine().trim();
                    List<Message> matchingMessages = messages.findStoredMessagesByRecipient(recipient);
                    if (matchingMessages.isEmpty()) {
                        System.out.println("No stored messages found for that recipient.");
                    } else {
                        displayMessages(matchingMessages);
                    }
                }
                case "5" -> {
                    System.out.print("Enter message hash: ");
                    String hash = scan.nextLine().trim();
                    try {
                        messages.deleteStoredMessageByHash(hash).ifPresentOrElse(
                                message -> System.out.println("Message: \"" + message.getMessageText()
                                        + "\" successfully deleted."),
                                () -> System.out.println("Message hash not found."));
                    } catch (IOException exception) {
                        System.out.println("Unable to delete stored message: " + exception.getMessage());
                    }
                }
                case "6" -> {
                    String report = messages.createStoredMessagesReport();
                    System.out.println(report.isEmpty() ? "No stored messages." : report);
                }
                case "0" -> {
                    return;
                }
                default -> System.out.println("Invalid option. Choose 0 through 6.");
            }
        }
    }
}