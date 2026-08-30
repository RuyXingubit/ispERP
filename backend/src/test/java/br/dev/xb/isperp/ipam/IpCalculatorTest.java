package br.dev.xb.isperp.ipam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IpCalculatorTest {

    private IpCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new IpCalculator();
    }

    @Test
    @DisplayName("Deve calcular métricas de sub-rede IPv4 /24 corretamente")
    void testCalculateSubnetIpv4Standard() {
        SubnetCalculationResult result = calculator.calculateSubnet("200.150.10.0/24");

        assertThat(result.getIpVersion()).isEqualTo(IpamIpVersion.IPV4);
        assertThat(result.getNetworkAddress()).isEqualTo("200.150.10.0");
        assertThat(result.getBroadcastAddress()).isEqualTo("200.150.10.255");
        assertThat(result.getNetmask()).isEqualTo("255.255.255.0");
        assertThat(result.getFirstUsableIp()).isEqualTo("200.150.10.1");
        assertThat(result.getLastUsableIp()).isEqualTo("200.150.10.254");
        assertThat(result.getTotalHosts()).isEqualTo(256);
        assertThat(result.getUsableHosts()).isEqualTo(254);
    }

    @Test
    @DisplayName("Deve calcular sub-rede ponto-a-ponto /31 (RFC 3021)")
    void testCalculateSubnetIpv4Slash31() {
        SubnetCalculationResult result = calculator.calculateSubnet("10.0.0.0/31");

        assertThat(result.getTotalHosts()).isEqualTo(2);
        assertThat(result.getUsableHosts()).isEqualTo(2);
        assertThat(result.getFirstUsableIp()).isEqualTo("10.0.0.0");
        assertThat(result.getLastUsableIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("Deve calcular host único /32")
    void testCalculateSubnetIpv4Slash32() {
        SubnetCalculationResult result = calculator.calculateSubnet("200.150.10.5/32");

        assertThat(result.getTotalHosts()).isEqualTo(1);
        assertThat(result.getUsableHosts()).isEqualTo(1);
        assertThat(result.getFirstUsableIp()).isEqualTo("200.150.10.5");
        assertThat(result.getLastUsableIp()).isEqualTo("200.150.10.5");
    }

    @Test
    @DisplayName("Deve calcular prefixo IPv6 /56 corretamente")
    void testCalculateSubnetIpv6() {
        SubnetCalculationResult result = calculator.calculateSubnet("2804:192c:100:1000::/56");

        assertThat(result.getIpVersion()).isEqualTo(IpamIpVersion.IPV6);
        assertThat(result.getPrefixLength()).isEqualTo(56);
        assertThat(result.getFirstUsableIp()).isEqualTo("2804:192c:100:1000::");
        assertThat(result.getBroadcastAddress()).isNull();
    }

    @Test
    @DisplayName("Deve dividir (split) um bloco /24 em múltiplos blocos /28")
    void testSplitSubnet() {
        List<SubnetCalculationResult> subnets = calculator.splitSubnet("200.150.10.0/24", 28);

        // 2^(28-24) = 2^4 = 16 subnets
        assertThat(subnets).hasSize(16);
        assertThat(subnets.get(0).getCidr()).isEqualTo("200.150.10.0/28");
        assertThat(subnets.get(0).getFirstUsableIp()).isEqualTo("200.150.10.1");
        assertThat(subnets.get(0).getLastUsableIp()).isEqualTo("200.150.10.14");
        assertThat(subnets.get(1).getCidr()).isEqualTo("200.150.10.16/28");
        assertThat(subnets.get(15).getCidr()).isEqualTo("200.150.10.240/28");
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar split com prefixo menor que o atual")
    void testSplitInvalidPrefix() {
        assertThrows(IllegalArgumentException.class, () -> calculator.splitSubnet("200.150.10.0/24", 22));
    }

    @Test
    @DisplayName("Deve detectar sobreposição de sub-redes corretamente")
    void testOverlapDetection() {
        assertThat(calculator.isOverlap("200.150.10.0/24", "200.150.10.0/28")).isTrue();
        assertThat(calculator.isOverlap("200.150.10.0/24", "200.150.11.0/24")).isFalse();
        assertThat(calculator.isOverlap("100.64.0.0/16", "100.64.10.0/24")).isTrue();
    }

    @Test
    @DisplayName("Deve verificar contenção de IP em sub-rede")
    void testContainment() {
        assertThat(calculator.contains("200.150.10.0/24", "200.150.10.50")).isTrue();
        assertThat(calculator.contains("200.150.10.0/24", "200.150.11.1")).isFalse();
        assertThat(calculator.contains("2804:192c:100::/40", "2804:192c:100:10::/56")).isTrue();
    }
}
