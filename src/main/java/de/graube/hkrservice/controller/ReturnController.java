package de.graube.hkrservice.controller;

import de.graube.hkrservice.service.F15zReturnService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST-API fuer Rueckmeldedateien aus nachgelagerten Systemen.
 */
@RestController
@RequestMapping("/returns")
public class ReturnController {

    private final F15zReturnService service;

    public ReturnController(F15zReturnService service) {
        this.service = service;
    }

    /**
     * Nimmt eine Rueckmeldedatei fuer einen bereits versendeten Job entgegen.
     *
     * @param jobId Job-ID
     * @param content Inhalt der Rueckmeldedatei
     * @return Abschlusssignal
     */
    @PostMapping("/{jobId}")
    public Mono<Void> uploadReturn(
            @PathVariable String jobId,
            @RequestBody String content) {

        return service.processReturnFile(jobId, content);
    }
}