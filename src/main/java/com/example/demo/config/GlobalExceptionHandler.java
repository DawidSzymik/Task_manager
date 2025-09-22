// src/main/java/com/example/demo/config/GlobalExceptionHandler.java
package com.example.demo.config;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, RedirectAttributes redirectAttributes) {
        System.err.println("Błąd aplikacji: " + e.getMessage());
        e.printStackTrace();

        redirectAttributes.addFlashAttribute("error", "Wystąpił błąd systemowy: " + e.getMessage());
        return "redirect:/admin";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(RuntimeException e, RedirectAttributes redirectAttributes) {
        System.err.println("Błąd runtime: " + e.getMessage());

        redirectAttributes.addFlashAttribute("error", "Błąd operacji: " + e.getMessage());
        return "redirect:/admin";
    }
}