package com.healing.backend.repository;

import com.healing.backend.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByUserIdAndDateOrderByStartTimeAsc(Long userId, LocalDate date);

    List<Schedule> findByUserIdAndDateBetweenOrderByDateAscStartTimeAsc(
        Long userId, LocalDate start, LocalDate end);

    @Query("SELECT s FROM Schedule s WHERE s.user.id = :userId " +
           "AND s.date = :date AND s.slotType = 'WORKOUT' " +
           "AND s.status = 'PENDING' " +
           "ORDER BY s.startTime ASC")
    List<Schedule> getPendingWorkoutsForDate(@Param("userId") Long userId,
                                              @Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.user.id = :userId " +
           "AND s.date BETWEEN :start AND :end AND s.status = 'COMPLETED'")
    int getCompletedCountInRange(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    void deleteByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);
}
