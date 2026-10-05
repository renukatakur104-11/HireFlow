package com.hireflow.modules.student;

import com.hireflow.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillRepository skillRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getSkills(
            @RequestParam(required = false) String search) {
        List<String> skills;
        if (search != null && !search.isBlank()) {
            skills = skillRepository.findByNameContainingIgnoreCase(search.trim())
                    .stream().map(Skill::getName).sorted().collect(Collectors.toList());
        } else {
            skills = skillRepository.findAll()
                    .stream().map(Skill::getName).sorted().collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success(skills));
    }
}
