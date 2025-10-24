package com.universal.llm.benchmark.db.models.task;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkTask {
    @Id
    private Long id; // Autoincrement
    private String taskId; // UserFriendly Name, Unique
    private String category; // Main Category
    private String subcategory; // Sub-Category
    private String question; // User Prompt sent to LLM for processing
    private String excellent; // The most expected result
    private String good; // Good result, but not perfect
    private String pass; // Result that can be considered as correct
    private String incorrectAnswerDirection; // Anything that tells that response is wrong and is not acceptable
    private LocalDateTime createdAt;
}
