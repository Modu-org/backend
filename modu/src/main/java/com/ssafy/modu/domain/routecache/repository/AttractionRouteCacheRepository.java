package com.ssafy.modu.domain.routecache.repository;

import com.ssafy.modu.domain.routecache.entity.AttractionRouteCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttractionRouteCacheRepository extends JpaRepository<AttractionRouteCache, Long> {

    Optional<AttractionRouteCache> findByFromAttraction_IdAndToAttraction_IdAndProvider(
            Long fromAttractionId,
            Long toAttractionId,
            String provider
    );

    List<AttractionRouteCache> findByProviderAndFromAttraction_IdInAndToAttraction_IdIn(
            String provider,
            Collection<Long> fromAttractionIds,
            Collection<Long> toAttractionIds
    );
}