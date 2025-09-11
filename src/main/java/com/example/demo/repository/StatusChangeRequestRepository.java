package com.example.demo.repository;

import com.example.demo.model.StatusChangeRequest;
import com.example.demo.model.Task;
import com.example.demo.model.User;
import com.example.demo.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusChangeRequestRepository extends JpaRepository<StatusChangeRequest, Long> {

    List<StatusChangeRequest> findByTask(Task task);
    List<StatusChangeRequest> findByRequestedBy(User user);
    List<StatusChangeRequest> findByStatus(RequestStatus status);
    List<StatusChangeRequest> findByTaskInAndStatus(List<Task> tasks, RequestStatus status);
}