package br.dev.xb.isperp.repository;

import br.dev.xb.isperp.entity.RadAcct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RadAcctRepository extends JpaRepository<RadAcct, Long> {

    // Sessões ativas (online)
    List<RadAcct> findByAcctStopTimeIsNullOrderByAcctStartTimeDesc();

    Page<RadAcct> findByAcctStopTimeIsNull(Pageable pageable);

    // Sessões ativas por username
    Optional<RadAcct> findFirstByUsernameAndAcctStopTimeIsNullOrderByAcctStartTimeDesc(String username);

    // Histórico de sessões por username
    List<RadAcct> findByUsernameOrderByAcctStartTimeDesc(String username);

    // Investigação Marco Civil: Localizar sessão ativa para determinado IP em determinado instante no tempo
    @Query("""
        SELECT r FROM RadAcct r
        WHERE (r.framedIpAddress = :ip OR r.framedIpv6Prefix LIKE :ipPrefix OR r.delegatedIpv6Prefix LIKE :ipPrefix)
          AND r.acctStartTime <= :eventTime
          AND (r.acctStopTime IS NULL OR r.acctStopTime >= :eventTime)
        ORDER BY r.acctStartTime DESC
    """)
    List<RadAcct> findSessionByIpAndTimestamp(
            @Param("ip") String ip,
            @Param("ipPrefix") String ipPrefix,
            @Param("eventTime") OffsetDateTime eventTime
    );
}
