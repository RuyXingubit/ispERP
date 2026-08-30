package br.dev.xb.isperp.ipam;

import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv6.IPv6Address;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class IpCalculator {

    public SubnetCalculationResult calculateSubnet(String cidrInput) {
        IPAddressString addrString = new IPAddressString(cidrInput.trim());
        IPAddress address = addrString.getAddress();
        if (address == null) {
            throw new IllegalArgumentException("CIDR ou endereço IP inválido: " + cidrInput);
        }

        IPAddress networkAddress = address.toPrefixBlock();
        IpamIpVersion version = address.isIPv4() ? IpamIpVersion.IPV4 : IpamIpVersion.IPV6;
        int prefixLength = networkAddress.getNetworkPrefixLength() != null ? networkAddress.getNetworkPrefixLength() : (version == IpamIpVersion.IPV4 ? 32 : 128);

        String netStr = networkAddress.getLower().withoutPrefixLength().toCanonicalString();
        String broadStr = version == IpamIpVersion.IPV4 ? networkAddress.getUpper().withoutPrefixLength().toCanonicalString() : null;
        String netmaskStr = networkAddress.getNetworkMask().withoutPrefixLength().toCanonicalString();
        String wildcardStr = version == IpamIpVersion.IPV4 && networkAddress.getHostMask() != null 
                ? networkAddress.getHostMask().withoutPrefixLength().toCanonicalString() : null;

        BigInteger count = networkAddress.getCount();
        long totalHosts = count.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0 ? Long.MAX_VALUE : count.longValue();
        long usableHosts;
        String firstUsable;
        String lastUsable;

        if (version == IpamIpVersion.IPV4) {
            if (prefixLength == 32) {
                usableHosts = 1;
                firstUsable = netStr;
                lastUsable = netStr;
            } else if (prefixLength == 31) {
                usableHosts = 2;
                firstUsable = netStr;
                lastUsable = broadStr != null ? broadStr : netStr;
            } else {
                usableHosts = Math.max(0, totalHosts - 2);
                firstUsable = networkAddress.getLower().increment(1).withoutPrefixLength().toCanonicalString();
                lastUsable = networkAddress.getUpper().increment(-1).withoutPrefixLength().toCanonicalString();
            }
        } else {
            // IPv6
            usableHosts = totalHosts;
            firstUsable = networkAddress.getLower().withoutPrefixLength().toCanonicalString();
            lastUsable = networkAddress.getUpper().withoutPrefixLength().toCanonicalString();
        }

        return SubnetCalculationResult.builder()
                .cidr(networkAddress.toCanonicalString())
                .ipVersion(version)
                .networkAddress(netStr)
                .broadcastAddress(broadStr)
                .netmask(netmaskStr)
                .wildcardMask(wildcardStr)
                .firstUsableIp(firstUsable)
                .lastUsableIp(lastUsable)
                .prefixLength(prefixLength)
                .totalHosts(totalHosts)
                .usableHosts(usableHosts)
                .build();
    }

    public List<SubnetCalculationResult> splitSubnet(String parentCidr, int targetPrefixLength) {
        IPAddressString addrString = new IPAddressString(parentCidr.trim());
        IPAddress parent = addrString.getAddress();
        if (parent == null) {
            throw new IllegalArgumentException("CIDR inválido: " + parentCidr);
        }

        IPAddress parentBlock = parent.toPrefixBlock();
        int currentPrefix = parentBlock.getNetworkPrefixLength() != null ? parentBlock.getNetworkPrefixLength() : (parentBlock.isIPv4() ? 32 : 128);

        if (targetPrefixLength <= currentPrefix) {
            throw new IllegalArgumentException("O prefixo alvo (" + targetPrefixLength + ") deve ser maior que o prefixo atual (" + currentPrefix + ")");
        }

        int maxPrefix = parentBlock.isIPv4() ? 32 : 128;
        if (targetPrefixLength > maxPrefix) {
            throw new IllegalArgumentException("O prefixo alvo (" + targetPrefixLength + ") excede o limite máximo (" + maxPrefix + ")");
        }

        List<SubnetCalculationResult> results = new ArrayList<>();
        Iterator<? extends IPAddress> iterator = parentBlock.prefixBlockStream(targetPrefixLength).iterator();

        // Limit loop to prevent OOM on massive splits (e.g. /16 to /32)
        int maxResults = 1024;
        int count = 0;

        while (iterator.hasNext() && count < maxResults) {
            IPAddress child = iterator.next();
            results.add(calculateSubnet(child.toCanonicalString()));
            count++;
        }

        return results;
    }

    public boolean isOverlap(String cidrA, String cidrB) {
        IPAddress a = new IPAddressString(cidrA.trim()).getAddress();
        IPAddress b = new IPAddressString(cidrB.trim()).getAddress();
        if (a == null || b == null) {
            return false;
        }
        if (a.isIPv4() != b.isIPv4()) {
            return false;
        }
        return a.toPrefixBlock().contains(b.toPrefixBlock()) || b.toPrefixBlock().contains(a.toPrefixBlock());
    }

    public boolean contains(String parentCidr, String childCidrOrIp) {
        IPAddress parent = new IPAddressString(parentCidr.trim()).getAddress();
        IPAddress child = new IPAddressString(childCidrOrIp.trim()).getAddress();
        if (parent == null || child == null) {
            return false;
        }
        if (parent.isIPv4() != child.isIPv4()) {
            return false;
        }
        return parent.toPrefixBlock().contains(child);
    }
}
