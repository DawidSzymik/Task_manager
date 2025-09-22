// src/main/java/com/example/demo/model/MessageType.java
package com.example.demo.model;

public enum MessageType {
    TEXT,           // Zwykła wiadomość tekstowa
    SYSTEM,         // Wiadomość systemowa (np. "Użytkownik X dołączył do projektu")
    FILE,           // Udostępnienie pliku
    TASK_REFERENCE  // Odniesienie do zadania
}
