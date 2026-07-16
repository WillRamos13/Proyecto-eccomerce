package com.fastmarket.api.repository;

import com.fastmarket.api.model.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActivoTrue();
}
