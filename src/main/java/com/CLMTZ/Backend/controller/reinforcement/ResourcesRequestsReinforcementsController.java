package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.CLMTZ.Backend.dto.reinforcement.ResourcesRequestsReinforcementsDTO;
import com.CLMTZ.Backend.service.reinforcement.IResourcesRequestsReinforcementsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/resources-requests-reinforcements")
@RequiredArgsConstructor
public class ResourcesRequestsReinforcementsController {

    private final IResourcesRequestsReinforcementsService service;

    @GetMapping
    public ResponseEntity<List<ResourcesRequestsReinforcementsDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourcesRequestsReinforcementsDTO> findById(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<ResourcesRequestsReinforcementsDTO> save(@RequestBody ResourcesRequestsReinforcementsDTO dto) {
        return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourcesRequestsReinforcementsDTO> update(@PathVariable("id") Integer id, @RequestBody ResourcesRequestsReinforcementsDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
