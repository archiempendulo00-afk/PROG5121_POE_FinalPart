package com.mycompany.userregistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MessageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void messageLengthAcceptsUpTo250CharactersAndRejectsLongerMessages() {
        Message validMessage = new Message("0000000000", "+27718693002", "a".repeat(250), 0);
        Message invalidMessage = new Message("0000000000", "+27718693002", "a".repeat(251), 0);

        assertEquals("Message ready to send.", validMessage.validateMessage());
        assertEquals("Message exceeds 250 characters by 1; please reduce the size.",
                invalidMessage.validateMessage());
    }

    @Test
    void recipientMustUseAnInternationalNumber() {
        Message validMessage = new Message("0000000000", "+27718693002", "Hi", 0);
        Message invalidMessage = new Message("0000000000", "08575975889", "Hi", 0);

        assertEquals("Cell phone number successfully captured.", validMessage.checkRecipientCell());
        assertEquals("Cell phone number is incorrectly formatted or does not contain an international code. Please correct the number and try again.",
                invalidMessage.checkRecipientCell());
    }

    @Test
    void messageHashUsesIdPrefixMessageNumberAndFirstAndLastWords() {
        Message message = new Message("0000000000", "+27718693002",
                "Hi Mike, can you join us for dinner tonight?", 0);

        assertEquals("00:0:HITONIGHT", message.createMessageHash());
    }

    @Test
    void messageIdIsLimitedToTenCharacters() {
        assertTrue(new Message("1234567890", "+27718693002", "Hi", 0).checkMessageID());
        assertFalse(new Message("12345678901", "+27718693002", "Hi", 0).checkMessageID());
    }

    @Test
    void messageActionsSendDisregardAndStoreWithExpectedResponses() {
        Message sentMessage = new Message("0000000000", "+27718693002", "Hi", 0);
        Message discardedMessage = new Message("0000000001", "+27718693002", "Hello", 1);
        Message storedMessage = new Message("0000000002", "+27718693002", "Later", 2);
        int sentBefore = Message.returnTotalMessages();

        assertEquals("Message successfully sent.", sentMessage.SentMessage("1"));
        assertEquals("Press 0 to delete the message.", discardedMessage.SentMessage("2"));
        assertEquals("Message successfully stored.", storedMessage.SentMessage("3"));
        assertEquals(sentBefore + 1, Message.returnTotalMessages());
    }

    @Test
    void storedMessageIsAppendedAsEscapedJson() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("messages.json");
        Message firstMessage = new Message("0000000000", "+27718693002", "Hi, \"Mike\"", 0);
        Message secondMessage = new Message("0000000001", "+27718693002", "See you", 1);

        firstMessage.storeMessage(jsonFile);
        secondMessage.storeMessage(jsonFile);

        String json = Files.readString(jsonFile);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]\n"));
        assertTrue(json.contains("Hi, \\\"Mike\\\""));
        assertTrue(json.contains("\"messageID\":\"0000000001\""));
    }

    @Test
    void generatedMessageIdContainsExactlyTenDigits() {
        Message message = new Message();

        assertTrue(message.getMessageID().matches("\\d{10}"));
    }

    @Test
    void oneWordMessageHashUsesThatWordTwice() {
        Message message = new Message("1234567890", "+27718693002", "Hello!", 4);

        assertEquals("12:4:HELLOHELLO", message.createMessageHash());
    }

    @Test
    void invalidActionDoesNotIncrementSentMessageTotal() {
        Message message = new Message("0000000000", "+27718693002", "Hi", 0);
        int sentBefore = Message.returnTotalMessages();

        assertEquals("Invalid selection.", message.SentMessage("4"));
        assertEquals(sentBefore, Message.returnTotalMessages());
    }

    @Test
    void storingRejectsAFileThatIsNotAJsonArray() throws Exception {
        Path jsonFile = temporaryDirectory.resolve("invalid-messages.json");
        Files.writeString(jsonFile, "not-json");
        Message message = new Message("0000000000", "+27718693002", "Hi", 0);

        assertThrows(IOException.class, () -> message.storeMessage(jsonFile));
    }
}