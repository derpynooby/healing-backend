package com.healing.backend.repository;

import com.healing.backend.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUserIdAndDateOrderByCreatedAtAsc(Long userId, LocalDate date);

    @Query("SELECT COALESCE(SUM(w.xpEarned), 0) FROM WorkoutLog w " +
           "WHERE w.user.id = :userId AND w.completed = true " +
           "AND w.date BETWEEN :start AND :end")
    int getXpInRange(@Param("userId") Long userId,
                     @Param("start") LocalDate start,
                     @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(w.caloriesBurned), 0) FROM WorkoutLog w " +
           "WHERE w.user.id = :userId AND w.completed = true AND w.date = :date")
    int getCaloriesBurnedForDate(@Param("userId") Long userId,
                                 @Param("date") LocalDate date);

    @Query("SELECT COUNT(w) FROM WorkoutLog w " +
           "WHERE w.user.id = :userId AND w.completed = true " +
           "AND w.date BETWEEN :start AND :end")
    int getCompletedCountInRange(@Param("userId") Long userId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(w.durationMinutes), 0) FROM WorkoutLog w " +
           "WHERE w.user.id = :userId AND w.completed = true " +
           "AND w.date BETWEEN :start AND :end")
    int getTotalMinutesInRange(@Param("userId") Long userId,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(w.caloriesBurned), 0) FROM WorkoutLog w " +
           "WHERE w.user.id = :userId AND w.completed = true " +
           "AND w.date BETWEEN :start AND :end")
    int getTotalCaloriesBurnedInRange(@Param("userId") Long userId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}
