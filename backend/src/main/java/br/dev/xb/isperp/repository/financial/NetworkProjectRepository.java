package br.dev.xb.isperp.repository.financial;

import br.dev.xb.isperp.entity.financial.NetworkProject;
import br.dev.xb.isperp.entity.financial.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NetworkProjectRepository extends JpaRepository<NetworkProject, UUID> {
    List<NetworkProject> findByStatusOrderByStartDateDesc(ProjectStatus status);
    List<NetworkProject> findByCityIgnoreCase(String city);
}
