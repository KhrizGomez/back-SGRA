package com.CLMTZ.Backend.controller.academic;

// import java.util.List;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import com.CLMTZ.Backend.dto.academic.TeachingDTO;
// import com.CLMTZ.Backend.service.academic.ITeachingService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/academic/teachings")
@RequiredArgsConstructor
public class TeachingController {
    // private final ITeachingService service;

    // @GetMapping
    // public ResponseEntity<List<TeachingDTO>> findAll() { return ResponseEntity.ok(service.findAll()); }

    // @GetMapping("/{id}")
    // public ResponseEntity<TeachingDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(service.findById(id)); }

    // @PostMapping
    // public ResponseEntity<TeachingDTO> save(@RequestBody TeachingDTO dto) { return new ResponseEntity<>(service.save(dto), HttpStatus.CREATED); }

    // @PutMapping("/{id}")
    // public ResponseEntity<TeachingDTO> update(@PathVariable("id") Integer id, @RequestBody TeachingDTO dto) { return ResponseEntity.ok(service.update(id, dto)); }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { service.deleteById(id); return ResponseEntity.noContent().build(); }

}