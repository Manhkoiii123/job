package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.payload.request.AddProjectRequest;
import com.manh.job.payload.response.ProjectResponse;
import com.manh.job.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resumes/{resumeId}/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> addProject(
            @PathVariable("resumeId") Long resumeId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddProjectRequest req
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addProject(resumeId, candidateId, req));
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects(
            @PathVariable Long resumeId
    ) {
        return ResponseEntity.ok(projectService.getAllProjects(resumeId));
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long resumeId,
            @PathVariable Long projectId,
            @RequestHeader("X-User-Id") Long candidateId,
            @RequestBody @Valid AddProjectRequest req
    ) throws Exception {
        return ResponseEntity.ok(
                projectService.updateProject(projectId, resumeId, candidateId, req)
        );
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<ApiResponse> deleteProject(
            @PathVariable Long resumeId,
            @PathVariable Long projectId,
            @RequestHeader("X-User-Id") Long candidateId
    ) throws Exception {
        projectService.deleteProject(projectId, resumeId, candidateId);
        return ResponseEntity.ok(new ApiResponse("Project deleted successfully", true));
    }
}
