package com.disobaeva.hotelbooking.model.entity;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.model.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Клиент, оформляющий бронирования.
 *
 * <p>Связь «клиент может иметь несколько бронирований» — агрегация:
 * бронирования живут отдельно и не удаляются вместе с клиентом.
 */
@Entity
@Table(
        name = "clients",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_client_email",
                columnNames = "email"
        )
)
public class Client extends AbstractEntity {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @OneToMany(mappedBy = "client")
    private List<Booking> bookings = new ArrayList<>();

    /** Требуется Hibernate. */
    protected Client() {
    }

    public Client(String firstName, String lastName, String email) {
        setFirstName(firstName);
        setLastName(lastName);
        setEmail(email);
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public List<Booking> getBookings() {
        return Collections.unmodifiableList(bookings);
    }

    public void setFirstName(String firstName) {
        this.firstName = requireText(firstName, "Имя");
    }

    public void setLastName(String lastName) {
        this.lastName = requireText(lastName, "Фамилия");
    }

    public void setEmail(String email) {
        this.email = requireEmail(email);
    }

    public void addBooking(Booking booking) {
        if (booking == null) {
            throw new BusinessRuleException("Нельзя добавить null вместо бронирования");
        }
        bookings.add(booking);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(fieldName + " клиента не может быть пустым");
        }
        return value.trim();
    }

    private String requireEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessRuleException("Некорректный email: " + email);
        }
        return email.trim().toLowerCase();
    }

    @Override
    public String toString() {
        return "Client{id=" + getId() + ", name='" + getFullName() + "', email='" + email + "'}";
    }
}
