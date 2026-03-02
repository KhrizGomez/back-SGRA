package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ParticipantsDTO;
import com.CLMTZ.Backend.service.reinforcement.IParticipantsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/participants")
@RequiredArgsConstructor
public class ParticipantsController {

    private final IParticipantsService service;

    @GetMapping
    public ResponseEntity<List<ParticipantsDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<ParticipantsDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    @PostMapping
    public ResponseEntity<ParticipantsDTO> save(@RequestBody ParticipantsDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<ParticipantsDTO> update(@PathVariable("id") Integer id, @RequestBody ParticipantsDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }
}
