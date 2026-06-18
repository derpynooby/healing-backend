package com.healing.backend.repository;

import com.healing.backend.model.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    List<MealLog> findByUserIdAndDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    @Query("SELECT COALESCE(SUM(m.calories), 0) FROM MealLog m " +
           "WHERE m.user.id = :userId AND m.date = :date")
    int getTotalCaloriesForDate(@Param("userId") Long userId,
                                @Param("date") LocalDate date);

    @Query("SELECT m FROM MealLog m " +
           "WHERE m.user.id = :userId AND m.date = :date AND m.isCheatTreat = true")
    List<MealLog> getCheatMealsForDate(@Param("userId") Long userId,
                                       @Param("date") LocalDate date);
}
