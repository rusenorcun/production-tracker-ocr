package com.example.uretimveri.repository;

import com.example.uretimveri.model.HotCoil;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HotCoilRepository extends JpaRepository<HotCoil, Long> {
    @EntityGraph(attributePaths = "product")
    @Query("select h from HotCoil h join fetch h.product")
    List<HotCoil> findAllWithProduct();
}
