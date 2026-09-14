package com.example.MyFirstApp.repository.HRandCoummunicationRespository;

import com.example.MyFirstApp.Entity.HRandCoummunicationEntity.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepositroy
        extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

}