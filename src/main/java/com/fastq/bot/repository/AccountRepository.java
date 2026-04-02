package com.fastq.bot.repository;

import com.fastq.bot.entity.AccountEntity;
import com.fastq.bot.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for {@link AccountEntity}.
 * <p>
 * Provides CRUD operations plus custom finders for the automation workflow.
 */
@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    /**
     * Find all accounts with a specific status.
     * Used to load IDLE/WAITING accounts for processing.
     */
    List<AccountEntity> findAllByStatus(AccountStatus status);

    /**
     * Find all accounts whose status is NOT the given value.
     * Typically used as {@code findAllByStatusNot(BOOKED)} to get all processable accounts.
     */
    List<AccountEntity> findAllByStatusNot(AccountStatus status);

    /**
     * Find an account by its email address.
     */
    Optional<AccountEntity> findByEmail(String email);
}
