package com.example.demo.controller;

import com.example.demo.model.UploadedFile;
import com.example.demo.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // <-- To jest ważne!
public class FileDebugController {

    @Autowired
    private FileService fileService;

    @GetMapping("/debug/files")
    public List<UploadedFile> allFiles() {
        return fileService.getFilesForTask(1L); // albo inny taskId
    }
}
