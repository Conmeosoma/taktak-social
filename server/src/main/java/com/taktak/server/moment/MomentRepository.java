package com.taktak.server.moment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MomentRepository extends JpaRepository<Moment, UUID> { }
