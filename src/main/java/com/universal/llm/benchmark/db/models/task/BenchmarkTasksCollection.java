package com.universal.llm.benchmark.db.models.task;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BenchmarkTasksCollection {
    @Id
    private Long id;
    private String name; // Name of the collection
    private LocalDateTime createdAt;


    // One-to-Many relationship with BenchmarkTasksCollectionItem
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenchmarkTasksCollectionItem> items;
}
