package com.manh.job.service;

import com.manh.job.payload.request.AddProjectRequest;
import com.manh.job.payload.response.ProjectResponse;

import java.util.List;

public interface ProjectService {
    ProjectResponse addProject(
            Long resumeId, Long candidateId, AddProjectRequest req
        ) throws Exception;

    List<ProjectResponse> getAllProjects(Long resumeId);

    ProjectResponse updateProject(Long projectId, Long resumeId,
                                  Long candidateId,
                                  AddProjectRequest req) throws Exception;

    void deleteProject(Long projectId, Long resumeId, Long candidateId) throws Exception;
}
