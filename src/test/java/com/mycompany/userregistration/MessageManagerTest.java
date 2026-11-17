package com.mycompany.userregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageManagerTest {

    @TempDir
    Path temporaryDirectory;

    private MessageManager manager;
    private Message sentMessageOne;
    private Message storedMessageTwo;
    private Message disregardedMessageThree;
    private Message sentMessageFour;
    private Message storedMessageFive;

    @BeforeEach
    void setUp() throws Exception {
        manager = new MessageManager(temporaryDirectory.resolve("messages.json"));
        sentMessageOne = message("1000000001", "+27834557896", "Did you get the cake?", 0);
        storedMessageTwo = message("2000000002", "+27838884567",
                "Where are you? You are late! I have asked you to be on time.", 1);
        disregardedMessageThree = message("3000000003", "+27834484567",
                "Yohoooo, I am at your gate.", 2);
        sentMessageFour = message("4000000004", "+27838884567", "It is dinner time!", 3);
        storedMessageFive = message("5000000005", "+27838884567",
                "Ok, I am leaving without you.", 4);

        manager.addSentMessage(sentMessageOne);
        manager.storeMessage(storedMessageTwo);
        manager.addDisregardedMessage(disregardedMessageThree);
        manager.addSentMessage(sentMessageFour);
        manager.storeMessage(storedMessageFive);
    }

    @Test
    void messageCollectionsArePopulatedFromTheirActions() {
        assertEquals(List.of(sentMessageOne, sentMessageFour), manager.getSentMessages());
        assertEquals(List.of(disregardedMessageThree), manager.getDisregardedMessages());
        assertEquals(List.of(storedMessageTwo, storedMessageFive), manager.getStoredMessages());
        assertEquals(List.of(storedMessageTwo.getMessageHash(), storedMessageFive.getMessageHash()),
                manager.getMessageHashes());
        assertEquals(List.of(storedMessageTwo.getMessageID(), storedMessageFive.getMessageID()),
                manager.getMessageIds());
    }

    @Test
    void jsonStoredMessagesLoadIntoCollectionsAndSearchArrays() throws Exception {
        MessageManager reloadedManager = new MessageManager(
                temporaryDirectory.resolve("messages.json"));

        assertEquals(List.of(storedMessageTwo.getMessageID(), storedMessageFive.getMessageID()),
                reloadedManager.getMessageIds());
        assertEquals(List.of(storedMessageTwo.getMessageHash(), storedMessageFive.getMessageHash()),
                reloadedManager.getMessageHashes());
        assertEquals(storedMessageTwo.getMessageText(),
                reloadedManager.findStoredMessageById(storedMessageTwo.getMessageID())
                        .orElseThrow().getMessageText());
    }

    @Test
    void longestStoredMessageIsReturned() {
        assertEquals(storedMessageTwo, manager.getLongestStoredMessage().orElseThrow());
    }

    @Test
    void searchesStoredMessagesByRecipient() {
        assertEquals(List.of(storedMessageTwo, storedMessageFive),
                manager.findStoredMessagesByRecipient("+27838884567"));
        assertTrue(manager.findStoredMessagesByRecipient("+27834557896").isEmpty());
    }

    @Test
    void messageCanBeDeletedByHashAndDeletionPersists() throws Exception {
        Message deleted = manager.deleteStoredMessageByHash(storedMessageTwo.getMessageHash())
                .orElseThrow();

        assertEquals(storedMessageTwo, deleted);
        assertFalse(manager.getStoredMessages().contains(storedMessageTwo));
        MessageManager reloadedManager = new MessageManager(
                temporaryDirectory.resolve("messages.json"));
        assertEquals(List.of(storedMessageFive.getMessageID()), reloadedManager.getMessageIds());
    }

    @Test
    void reportContainsAllStoredMessageDetails() {
        String report = manager.createStoredMessagesReport();

        assertTrue(report.contains("Message Hash: " + storedMessageTwo.getMessageHash()));
        assertTrue(report.contains("Message ID: " + storedMessageTwo.getMessageID()));
        assertTrue(report.contains("Recipient: +27838884567"));
        assertTrue(report.contains("Message: " + storedMessageTwo.getMessageText()));
        assertTrue(report.contains("Message Hash: " + storedMessageFive.getMessageHash()));
        assertTrue(report.contains("Message: " + storedMessageFive.getMessageText()));
    }

    private Message message(String id, String recipient, String text, int number) {
        return new Message(id, recipient, text, number);
    }
}
