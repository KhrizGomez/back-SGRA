package com.CLMTZ.Backend.controller.reinforcement.student;

import com.CLMTZ.Backend.config.UserContextHolder;
import com.CLMTZ.Backend.dto.reinforcement.student.NotificationChannelDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.student.StudentPreferenceUpsertResponseDTO;
import com.CLMTZ.Backend.dto.security.session.UserContext;
import com.CLMTZ.Backend.service.reinforcement.student.StudentPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/preferences")
public class StudentPreferenceController {

    private final StudentPreferenceService studentPreferenceService;

    public StudentPreferenceController(StudentPreferenceService studentPreferenceService) {
        this.studentPreferenceService = studentPreferenceService;
    }

    @GetMapping("/channels")
    public ResponseEntity<?> getActiveChannels() {
        try {
            List<NotificationChannelDTO> channels = studentPreferenceService.getActiveChannels();
            return ResponseEntity.ok(channels);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error retrieving channels: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyPreference() {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();

            StudentPreferenceDTO preference = studentPreferenceService.getMyPreference(userId);

            if (preference == null) {
                // Estudiante nuevo sin preferencia configurada
                return ResponseEntity.ok().build();
            }

            return ResponseEntity.ok(preference);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error retrieving preference: " + e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> saveMyPreference(@RequestBody StudentPreferenceUpsertRequestDTO req) {
        try {
            UserContext ctx = UserContextHolder.getContext();
            Integer userId = ctx.getUserId();


            if (req.getChannelId() == null || req.getChannelId() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid channelId parameter"));
            }

            if (req.getReminderAnticipation() == null || req.getReminderAnticipation() < 0) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid reminderAnticipation parameter"));
            }

            StudentPreferenceUpsertResponseDTO response = studentPreferenceService.saveMyPreference(userId, req);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Error saving preference: " + e.getMessage()));
        }
    }
}