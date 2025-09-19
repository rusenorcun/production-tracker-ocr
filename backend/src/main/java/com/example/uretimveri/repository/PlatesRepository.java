package com.example.uretimveri.repository;

import com.example.uretimveri.model.Plates;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlatesRepository extends JpaRepository<Plates, Long> {

    @EntityGraph(attributePaths = "product")
    @Query("select p from Plates p join fetch p.product")
    List<Plates> findAllWithProduct();
}
