package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.JobSkillRequest;
import com.manh.job.dto.response.JobSkillResponse;
import com.manh.job.service.JobSkillService;
import com.manh.job.util.AdminGuard;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/job-skills")
public class JobSkillController {

    private final JobSkillService skillService;
    private final AdminGuard adminGuard;

    @PostMapping
    public ResponseEntity<JobSkillResponse> createSkill(
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobSkillRequest jobSkillRequest
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(skillService.createSkill(jobSkillRequest));
    }

    @GetMapping
    public ResponseEntity<List<JobSkillResponse>> getAllSkills() {
        return ResponseEntity.ok(skillService.getAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobSkillResponse> getSkillById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(skillService.getSkillById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobSkillResponse> updateSkill(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody @Valid JobSkillRequest req
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        return ResponseEntity.ok(skillService.updateSkill(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteSkill(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId
    ) throws Exception {
        adminGuard.assertAdmin(adminId);
        skillService.deleteSkill(id);
        return ResponseEntity.ok(new ApiResponse("Skill deleted successfully", true));
    }
}
