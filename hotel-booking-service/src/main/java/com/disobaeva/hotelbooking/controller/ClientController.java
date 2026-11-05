package com.disobaeva.hotelbooking.controller;

import com.disobaeva.hotelbooking.dto.request.ClientRequest;
import com.disobaeva.hotelbooking.dto.response.ClientResponse;
import com.disobaeva.hotelbooking.model.entity.Client;
import com.disobaeva.hotelbooking.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** HTTP-адаптер для клиентов. */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    /** GET /api/clients */
    @GetMapping
    public List<ClientResponse> findAll() {
        return clientService.findAll().stream()
                .map(ClientResponse::from)
                .toList();
    }

    /** GET /api/clients/{id} */
    @GetMapping("/{id}")
    public ClientResponse findById(@PathVariable Long id) {
        return ClientResponse.from(clientService.getById(id));
    }

    /** POST /api/clients */
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        Client client = clientService.create(
                request.firstName(),
                request.lastName(),
                request.email());
        return ResponseEntity
                .created(URI.create("/api/clients/" + client.getId()))
                .body(ClientResponse.from(client));
    }

    /** PUT /api/clients/{id} */
    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable Long id,
                                 @Valid @RequestBody ClientRequest request) {
        Client client = clientService.update(
                id,
                request.firstName(),
                request.lastName(),
                request.email());
        return ClientResponse.from(client);
    }

    /** DELETE /api/clients/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        clientService.delete(id);
    }
}
