package com.example.uretimveri.repository;

import com.example.uretimveri.model.ColdCoil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColdCoilRepository extends JpaRepository<ColdCoil, Long> {
    @EntityGraph(attributePaths = "product")
    @Query("select c from ColdCoil c join fetch c.product")
    List<ColdCoil> findAllWithProduct();
}
