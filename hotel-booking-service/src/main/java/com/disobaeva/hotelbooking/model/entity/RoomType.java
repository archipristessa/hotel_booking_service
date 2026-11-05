package com.disobaeva.hotelbooking.model.entity;

import com.disobaeva.hotelbooking.exception.BusinessRuleException;
import com.disobaeva.hotelbooking.model.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Тип номера: «Стандарт», «Семейный», «Люкс» и т.п. */
@Entity
@Table(
        name = "room_types",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_type_name",
                columnNames = "name"
        )
)
public class RoomType extends AbstractEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** Требуется Hibernate. */
    protected RoomType() {
    }

    public RoomType(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    /** Название типа номера не может быть пустым. */
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("Название типа номера не может быть пустым");
        }
        this.name = name.trim();
    }

    @Override
    public String toString() {
        return "RoomType{id=" + getId() + ", name='" + name + "'}";
    }
}
