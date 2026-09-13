package com.manh.job.service;

import com.manh.job.dto.request.AddApplicationNoteRequest;
import com.manh.job.dto.response.ApplicationNoteResponse;

import java.util.List;

public interface ApplicationNoteService {
    ApplicationNoteResponse addNote(
            Long applicationId, Long employerId, AddApplicationNoteRequest req
    ) throws Exception;

    List<ApplicationNoteResponse> getNotesByApplication(
            Long applicationId, Long employerId
    );

    void deleteNote(Long applicationId, Long noteId, Long employerId) throws Exception;
}
