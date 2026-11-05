package com.disobaeva.hotelbooking.service;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.exception.ClientNotFoundException;
import com.disobaeva.hotelbooking.exception.DuplicateEntityException;
import com.disobaeva.hotelbooking.model.entity.Client;
import com.disobaeva.hotelbooking.repository.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Бизнес-логика по клиентам. */
@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /** Создаёт клиента. Email должен быть корректным и уникальным. */
    @Transactional
    public Client create(String firstName, String lastName, String email) {
        if (clientRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEntityException("Клиент с email '" + email + "' уже существует");
        }
        return clientRepository.save(new Client(firstName, lastName, email));
    }

    public Client getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException(id));
    }

    public Optional<Client> getByEmail(String email) {
        return clientRepository.findByEmailIgnoreCase(email);
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    /** Изменение клиента. Email остаётся уникальным. */
    @Transactional
    public Client update(Long id, String firstName, String lastName, String email) {
        Client client = getById(id);
        boolean emailChanged = !client.getEmail().equalsIgnoreCase(email);
        if (emailChanged && clientRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEntityException("Email '" + email + "' уже занят другим клиентом");
        }
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        return clientRepository.save(client);
    }

    /**
     * Удаление клиента. Клиента с бронированиями удалять нельзя:
     * история бронирований должна сохраниться.
     */
    @Transactional
    public void delete(Long id) {
        Client client = getById(id);
        if (!client.getBookings().isEmpty()) {
            throw new BusinessRuleException(
                    "Нельзя удалить клиента '" + client.getFullName() + "': у него есть бронирования");
        }
        clientRepository.delete(client);
    }
}
