package com.CLMTZ.Backend.controller.reinforcement;

import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.CLMTZ.Backend.dto.reinforcement.ListOfWorkAreaRequestsRequestDTO;
import com.CLMTZ.Backend.dto.reinforcement.OnSiteReinforcementDTO;
import com.CLMTZ.Backend.service.reinforcement.IOnSiteReinforcementService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reinforcement/on-site-reinforcements")
@RequiredArgsConstructor
public class OnSiteReinforcementController {

    private final IOnSiteReinforcementService onSiteReinforcementSer;

    @GetMapping
    public ResponseEntity<List<OnSiteReinforcementDTO>> findAll() { return ResponseEntity.ok(onSiteReinforcementSer.findAll()); }

    @GetMapping("/{id}")
    public ResponseEntity<OnSiteReinforcementDTO> findById(@PathVariable("id") Integer id) { return ResponseEntity.ok(onSiteReinforcementSer.findById(id)); }

    @PostMapping
    public ResponseEntity<OnSiteReinforcementDTO> save(@RequestBody OnSiteReinforcementDTO dto) { return new ResponseEntity<>(onSiteReinforcementSer.save(dto), HttpStatus.CREATED); }

    @PutMapping("/{id}")
    public ResponseEntity<OnSiteReinforcementDTO> update(@PathVariable("id") Integer id, @RequestBody OnSiteReinforcementDTO dto) { return ResponseEntity.ok(onSiteReinforcementSer.update(id, dto)); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) { onSiteReinforcementSer.deleteById(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/list-areas-ofsite")
    public ResponseEntity<List<ListOfWorkAreaRequestsRequestDTO>> listAreasRequests(@RequestParam Integer userId){
        List<ListOfWorkAreaRequestsRequestDTO> listAreasRequesteOfSite = onSiteReinforcementSer.listAreasRequests(userId);
        return ResponseEntity.ok(listAreasRequesteOfSite);
    }
}
