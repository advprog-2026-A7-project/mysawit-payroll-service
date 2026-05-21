package com.mysawit.payroll.repository;

import com.mysawit.payroll.model.UserReplica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReplicaRepository extends JpaRepository<UserReplica, String> {
}
