package com.dartcodes.smartorder.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dartcodes.smartorder.model.Branch;
import com.dartcodes.smartorder.repository.BranchRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchRepository branchRepository;

    @GetMapping
    public ResponseEntity<List<Branch>> getActiveBranches() {
        return ResponseEntity.ok(branchRepository.findByIsActiveTrue());
    }
}
