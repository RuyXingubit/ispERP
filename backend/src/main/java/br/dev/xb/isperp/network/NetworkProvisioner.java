package br.dev.xb.isperp.network;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import org.springframework.lang.NonNull;

public interface NetworkProvisioner {

    /**
     * Identificador do tipo de driver.
     */
    NetworkDriverType getDriverType();

    /**
     * Provisiona e autoriza uma ONU/ONT na OLT ou concentrador.
     */
    boolean provisionOnu(@NonNull OnuProvisionRequest request, @NonNull NetworkDevice device);

    /**
     * Remove o provisionamento de uma ONU da rede.
     */
    boolean deprovisionOnu(@NonNull String onuMac, @NonNull NetworkDevice device);

    /**
     * Bloqueia o acesso à internet por inadimplência (corta tráfego ou reduz velocidade).
     */
    boolean blockInternetAccess(@NonNull String onuMac, String reason, @NonNull NetworkDevice device);

    /**
     * Desbloqueia o acesso integral à internet após confirmação de pagamento.
     */
    boolean unblockInternetAccess(@NonNull String onuMac, @NonNull NetworkDevice device);

    /**
     * Consulta sinal óptico (dBm) e status operacional da ONU na OLT.
     */
    OnuStatusResponse checkOnuStatus(@NonNull String onuMac, @NonNull NetworkDevice device);
}
