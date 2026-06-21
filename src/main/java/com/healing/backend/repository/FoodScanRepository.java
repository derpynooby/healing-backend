package com.healing.backend.repository;

import com.healing.backend.model.FoodScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodScanRepository extends JpaRepository<FoodScan, Long> {

    List<FoodScan> findByUserIdAndDateOrderByCreatedAtDesc(Long userId, LocalDate date);

    List<FoodScan> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(f.totalCalories), 0) FROM FoodScan f " +
           "WHERE f.user.id = :userId AND f.date = :date AND f.loggedToMeal = true")
    int getTotalScannedCaloriesForDate(@Param("userId") Long userId,
                                       @Param("date") LocalDate date);
}
