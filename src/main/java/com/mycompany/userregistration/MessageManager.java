package com.mycompany.userregistration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Maintains message collections and persists stored messages as a JSON array. */
public final class MessageManager {

    private final Path storedMessagesFile;
    private final List<Message> sentMessages = new ArrayList<>();
    private final List<Message> disregardedMessages = new ArrayList<>();
    private final List<Message> storedMessages = new ArrayList<>();

    public MessageManager(Path storedMessagesFile) throws IOException {
        this.storedMessagesFile = storedMessagesFile;
        loadStoredMessages();
    }

    public void addSentMessage(Message message) {
        sentMessages.add(message);
    }

    public void addDisregardedMessage(Message message) {
        disregardedMessages.add(message);
    }

    public void storeMessage(Message message) throws IOException {
        storedMessages.add(message);
        try {
            writeStoredMessages();
        } catch (IOException exception) {
            storedMessages.remove(storedMessages.size() - 1);
            throw exception;
        }
    }

    public List<Message> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public List<Message> getDisregardedMessages() {
        return Collections.unmodifiableList(disregardedMessages);
    }

    public List<Message> getStoredMessages() {
        return Collections.unmodifiableList(storedMessages);
    }

    public List<String> getMessageHashes() {
        return storedMessages.stream().map(Message::getMessageHash).toList();
    }

    public List<String> getMessageIds() {
        return storedMessages.stream().map(Message::getMessageID).toList();
    }

    public Optional<Message> getLongestStoredMessage() {
        return storedMessages.stream()
                .max((first, second) -> Integer.compare(
                        first.getMessageText().length(), second.getMessageText().length()));
    }

    public Optional<Message> findStoredMessageById(String messageId) {
        return storedMessages.stream()
                .filter(message -> message.getMessageID().equals(messageId))
                .findFirst();
    }

    public List<Message> findStoredMessagesByRecipient(String recipient) {
        return storedMessages.stream()
                .filter(message -> message.getRecipient().equals(recipient))
                .toList();
    }

    public Optional<Message> deleteStoredMessageByHash(String messageHash) throws IOException {
        for (int index = 0; index < storedMessages.size(); index++) {
            Message message = storedMessages.get(index);
            if (message.getMessageHash().equals(messageHash)) {
                storedMessages.remove(index);
                try {
                    writeStoredMessages();
                } catch (IOException exception) {
                    storedMessages.add(index, message);
                    throw exception;
                }
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    public String createStoredMessagesReport() {
        StringBuilder report = new StringBuilder();
        for (Message message : storedMessages) {
            if (!report.isEmpty()) {
                report.append(System.lineSeparator());
            }
            report.append("Message Hash: ").append(message.getMessageHash())
                    .append(System.lineSeparator())
                    .append("Message ID: ").append(message.getMessageID())
                    .append(System.lineSeparator())
                    .append("Recipient: ").append(message.getRecipient())
                    .append(System.lineSeparator())
                    .append("Message: ").append(message.getMessageText());
        }
        return report.toString();
    }

    private void loadStoredMessages() throws IOException {
        if (!Files.exists(storedMessagesFile)) {
            return;
        }
        String json = Files.readString(storedMessagesFile, StandardCharsets.UTF_8);
        List<Map<String, String>> records = new JsonArrayParser(json).parse();
        int messageNumber = 0;
        for (Map<String, String> record : records) {
            String id = requiredField(record, "messageID");
            String hash = requiredField(record, "messageHash");
            String recipient = requiredField(record, "recipient");
            String text = requiredField(record, "message");
            Message message = new Message(id, recipient, text, messageNumber++);
            message.setMessageHash(hash);
            storedMessages.add(message);
        }
    }

    private String requiredField(Map<String, String> record, String field) throws IOException {
        String value = record.get(field);
        if (value == null) {
            throw new IOException("Stored message is missing the '" + field + "' field.");
        }
        return value;
    }

    private void writeStoredMessages() throws IOException {
        StringBuilder json = new StringBuilder("[");
        for (Message message : storedMessages) {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append(System.lineSeparator()).append("  {")
                    .append("\"messageID\":\"").append(escapeJson(message.getMessageID()))
                    .append("\",\"messageHash\":\"").append(escapeJson(message.getMessageHash()))
                    .append("\",\"recipient\":\"").append(escapeJson(message.getRecipient()))
                    .append("\",\"message\":\"").append(escapeJson(message.getMessageText()))
                    .append("\"}");
        }
        if (!storedMessages.isEmpty()) {
            json.append(System.lineSeparator());
        }
        json.append("]").append(System.lineSeparator());
        Files.writeString(storedMessagesFile, json, StandardCharsets.UTF_8);
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (char character : value.toCharArray()) {
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static final class JsonArrayParser {

        private final String input;
        private int position;

        private JsonArrayParser(String input) {
            this.input = input;
        }

        private List<Map<String, String>> parse() throws IOException {
            List<Map<String, String>> records = new ArrayList<>();
            skipWhitespace();
            expect('[');
            skipWhitespace();
            if (consume(']')) {
                ensureEnd();
                return records;
            }
            do {
                skipWhitespace();
                records.add(parseObject());
                skipWhitespace();
                if (consume(']')) {
                    ensureEnd();
                    return records;
                }
                expect(',');
            } while (true);
        }

        private Map<String, String> parseObject() throws IOException {
            Map<String, String> fields = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return fields;
            }
            do {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                String value = parseString();
                if (fields.put(key, value) != null) {
                    throw error("Duplicate object field");
                }
                skipWhitespace();
                if (consume('}')) {
                    return fields;
                }
                expect(',');
            } while (true);
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < input.length()) {
                char character = input.charAt(position++);
                if (character == '"') {
                    return value.toString();
                }
                if (character < 0x20) {
                    throw error("Unescaped control character in string");
                }
                if (character != '\\') {
                    value.append(character);
                    continue;
                }
                if (position == input.length()) {
                    throw error("Incomplete escape sequence");
                }
                char escape = input.charAt(position++);
                switch (escape) {
                    case '"', '\\', '/' -> value.append(escape);
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> value.append(parseUnicodeEscape());
                    default -> throw error("Invalid escape sequence");
                }
            }
            throw error("Unterminated string");
        }

        private char parseUnicodeEscape() throws IOException {
            if (position + 4 > input.length()) {
                throw error("Incomplete Unicode escape");
            }
            String digits = input.substring(position, position + 4);
            try {
                char decoded = (char) Integer.parseInt(digits, 16);
                position += 4;
                return decoded;
            } catch (NumberFormatException exception) {
                throw error("Invalid Unicode escape");
            }
        }

        private void expect(char expected) throws IOException {
            if (!consume(expected)) {
                throw error("Expected '" + expected + "'");
            }
        }

        private boolean consume(char expected) {
            if (position < input.length() && input.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
                position++;
            }
        }

        private void ensureEnd() throws IOException {
            skipWhitespace();
            if (position != input.length()) {
                throw error("Unexpected content after JSON array");
            }
        }

        private IOException error(String message) {
            return new IOException(message + " at character " + position + ".");
        }
    }
}
