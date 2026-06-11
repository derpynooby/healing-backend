package com.healing.backend.repository;

import com.healing.backend.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);

    @Query("SELECT c FROM ChatMessage c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    List<ChatMessage> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    void deleteByUserId(Long userId);
}
