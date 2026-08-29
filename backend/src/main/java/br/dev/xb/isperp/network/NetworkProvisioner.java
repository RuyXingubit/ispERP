package br.dev.xb.isperp.network;

import br.dev.xb.isperp.entity.NetworkDevice;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;

public interface NetworkProvisioner {

    /**
     * Identificador do tipo de driver.
     */
    NetworkDriverType getDriverType();

    /**
     * Provisiona e autoriza uma ONU/ONT na OLT ou concentrador.
     */
    boolean provisionOnu(OnuProvisionRequest request, NetworkDevice device);

    /**
     * Remove o provisionamento de uma ONU da rede.
     */
    boolean deprovisionOnu(String onuMac, NetworkDevice device);

    /**
     * Bloqueia o acesso à internet por inadimplência (corta tráfego ou reduz velocidade).
     */
    boolean blockInternetAccess(String onuMac, String reason, NetworkDevice device);

    /**
     * Desbloqueia o acesso integral à internet após confirmação de pagamento.
     */
    boolean unblockInternetAccess(String onuMac, NetworkDevice device);

    /**
     * Consulta sinal óptico (dBm) e status operacional da ONU na OLT.
     */
    OnuStatusResponse checkOnuStatus(String onuMac, NetworkDevice device);
}
