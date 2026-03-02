package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.WorkAreaDTO;
import com.CLMTZ.Backend.service.reinforcement.IWorkAreaService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/work-areas")
@RequiredArgsConstructor
public class WorkAreaController {

    private final IWorkAreaService workAreaSer;

    @GetMapping
    public ResponseEntity<List<WorkAreaDTO>> findAll() { return ResponseEntity.ok(workAreaSer.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<WorkAreaDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(workAreaSer.findById(id)); }

    @PostMapping
    public ResponseEntity<WorkAreaDTO> save(@RequestBody WorkAreaDTO dto) { return new ResponseEntity<>(workAreaSer.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<WorkAreaDTO> update(@PathVariable("id") Integer id, @RequestBody WorkAreaDTO dto) { return ResponseEntity.ok(workAreaSer.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { workAreaSer.deleteById(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/list-workAreas")
    public ResponseEntity<List<WorkAreaDTO>> listWorkAreas(@RequestParam Integer academicTypeId){
        List<WorkAreaDTO> requestListAreas = workAreaSer.listAreasNames(academicTypeId);
        return ResponseEntity.ok(requestListAreas);
    }
}
