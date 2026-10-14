package com.studentfinance.controller;

import com.studentfinance.dto.CategoryResponse;
import com.studentfinance.entity.TransactionType;
import com.studentfinance.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(@RequestParam(required = false) TransactionType type) {
        List<CategoryResponse> categories = categoryService.getAll(type);
        return ResponseEntity.ok(categories);
    }
}
