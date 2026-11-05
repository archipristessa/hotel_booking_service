package com.disobaeva.hotelbooking.dto.response;

import com.disobaeva.hotelbooking.model.entity.Client;

/** Ответ по клиенту без списка бронирований. */
public record ClientResponse(
        Long id,
        String firstName,
        String lastName,
        String email
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getFirstName(),
                client.getLastName(),
                client.getEmail()
        );
    }
}
