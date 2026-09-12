package com.manh.job.service.impl;

import com.manh.job.mapper.ResumeMapper;
import com.manh.job.modal.Project;
import com.manh.job.modal.Resume;
import com.manh.job.payload.request.AddProjectRequest;
import com.manh.job.payload.response.ProjectResponse;
import com.manh.job.repository.ProjectRepository;
import com.manh.job.service.ProjectService;
import com.manh.job.service.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final ResumeService resumeService;

    @Override
    public ProjectResponse addProject(Long resumeId, Long candidateId, AddProjectRequest req) throws Exception {
        Resume resume = resumeService.getResumeEntity(resumeId);
        assertOwner(resume, candidateId);

        Project project = Project.builder()
                .resume(resume)
                .title(req.getTitle())
                .description(req.getDescription())
                .technologies(req.getTechnologies())
                .projectUrl(req.getProjectUrl())
                .sourceCodeUrl(req.getSourceCodeUrl())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .isOngoing(Boolean.TRUE.equals(req.getIsOngoing()))
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .build();

        return ResumeMapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    public List<ProjectResponse> getAllProjects(Long resumeId) {
        return projectRepository.findByResume_IdOrderByDisplayOrderAsc(resumeId)
                .stream().map(ResumeMapper::toProjectResponse).toList();
    }

    @Override
    public ProjectResponse updateProject(Long projectId, Long resumeId, Long candidateId, AddProjectRequest req)
            throws Exception {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new Exception("Project not found")
        );
        assertBelongsToResume(project, resumeId);
        assertOwner(project.getResume(), candidateId);

        project.setTitle(req.getTitle());
        project.setDescription(req.getDescription());
        project.setTechnologies(req.getTechnologies());
        project.setProjectUrl(req.getProjectUrl());
        project.setSourceCodeUrl(req.getSourceCodeUrl());
        project.setStartDate(req.getStartDate());
        project.setEndDate(req.getEndDate());
        project.setIsOngoing(Boolean.TRUE.equals(req.getIsOngoing()));
        if (req.getDisplayOrder() != null) {
            project.setDisplayOrder(req.getDisplayOrder());
        }

        return ResumeMapper.toProjectResponse(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long projectId, Long resumeId, Long candidateId) throws Exception {
        Project project = projectRepository.findById(projectId).orElseThrow(
                () -> new Exception("Project not found")
        );
        assertBelongsToResume(project, resumeId);
        assertOwner(project.getResume(), candidateId);
        projectRepository.delete(project);
    }

    private void assertBelongsToResume(Project project, Long resumeId) throws Exception {
        if (!project.getResume().getId().equals(resumeId)) {
            throw new Exception("Project not found");
        }
    }

    private void assertOwner(Resume resume, Long candidateId) throws Exception {
        if (!resume.getCandidateId().equals(candidateId)) {
            throw new Exception("Not found");
        }
    }
}
