package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaManagerDTO;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaManagerService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-area-manager")
@RequiredArgsConstructor
public class WorkAreaManagerController {

    private final IWorkAreaManagerService service;

    @GetMapping
    public ResponseEntity<List<WorkAreaManagerDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkAreaManagerDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<WorkAreaManagerDTO> save(@RequestBody WorkAreaManagerDTO dto) {
        return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkAreaManagerDTO> update(@PathVariable("id") Integer id, @RequestBody WorkAreaManagerDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
