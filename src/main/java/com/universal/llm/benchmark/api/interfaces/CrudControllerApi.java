package com.universal.llm.benchmark.api.interfaces;

import com.universal.llm.benchmark.api.types.ApiResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface CrudControllerApi<T> {
    ApiResponse<T> create(T entity);

    ApiResponse<T> get(Long id);

    ApiResponse<T> update(T entity);

    ResponseEntity<Void> delete(Long id);

    ApiResponse<List<T>> getAll();
}
