package com.disobaeva.hotelbooking.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.Hibernate;

/**
 * Общий предок всех JPA-сущностей.
 *
 * <p>{@link MappedSuperclass} означает: у этого класса нет своей таблицы, но его
 * поля наследуются всеми сущностями. Поэтому {@code id} и правила сравнения
 * описываются один раз.
 *
 * <p>Идентификатор выдаёт база данных (стратегия {@link GenerationType#IDENTITY},
 * в PostgreSQL — {@code BIGSERIAL}). До сохранения {@code id == null}.
 *
 * <p>Правила {@code equals}/{@code hashCode} учитывают прокси Hibernate:
 * {@link Hibernate#getClass(Object)} возвращает настоящий класс сущности даже
 * для прокси-объекта. {@code hashCode} специально не зависит от {@code id}:
 * id появляется только после сохранения, и если бы хеш менялся, объект «терялся»
 * бы в {@code HashSet}/{@code HashMap}.
 */
@MappedSuperclass
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    /**
     * Две сущности равны, если у них одинаковый настоящий тип и одинаковый
     * (не {@code null}) идентификатор. Новые, ещё не сохранённые сущности
     * равны только сами себе.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AbstractEntity that)) {
            return false;
        }
        if (Hibernate.getClass(this) != Hibernate.getClass(that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    /**
     * Хеш стабилен на протяжении всей жизни объекта: считается по классу,
     * а не по id, который появляется после сохранения.
     */
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
