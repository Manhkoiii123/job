package com.manh.job.controller;

import com.manh.job.dto.ApiResponse;
import com.manh.job.dto.request.AddApplicationNoteRequest;
import com.manh.job.dto.response.ApplicationNoteResponse;
import com.manh.job.service.ApplicationNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/notes")
@RequiredArgsConstructor
public class ApplicationNoteController {

    private final ApplicationNoteService applicationNoteService;

    @PostMapping
    public ResponseEntity<ApplicationNoteResponse> addNote(
	    @PathVariable Long applicationId,
	    @RequestHeader("X-User-Id") Long employerId,
	    @RequestBody @Valid AddApplicationNoteRequest req
    ) throws Exception {
	return ResponseEntity.status(HttpStatus.CREATED)
		.body(applicationNoteService.addNote(applicationId, employerId, req));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationNoteResponse>> getNotesByApplication(
	    @PathVariable Long applicationId,
	    @RequestHeader("X-User-Id") Long employerId
    ) {
	return ResponseEntity.ok(
		applicationNoteService.getNotesByApplication(applicationId, employerId));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<ApiResponse> deleteNote(
	    @PathVariable Long applicationId,
	    @PathVariable Long noteId,
	    @RequestHeader("X-User-Id") Long employerId
    ) throws Exception {
	applicationNoteService.deleteNote(applicationId, noteId, employerId);
	return ResponseEntity.ok(new ApiResponse("Application note deleted successfully", true));
    }
}
