package br.dev.xb.isperp.ipam;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class IpCalculator {

    public SubnetCalculationResult calculateSubnet(String cidrInput) {
        String clean = cidrInput.trim();
        String ipPart;
        int prefixLength;

        if (clean.contains("/")) {
            String[] parts = clean.split("/");
            ipPart = parts[0].trim();
            prefixLength = Integer.parseInt(parts[1].trim());
        } else {
            ipPart = clean;
            prefixLength = clean.contains(":") ? 128 : 32;
        }

        try {
            InetAddress addr = InetAddress.getByName(ipPart);
            byte[] bytes = addr.getAddress();
            boolean isIpv4 = bytes.length == 4;
            int totalBits = isIpv4 ? 32 : 128;
            IpamIpVersion version = isIpv4 ? IpamIpVersion.IPV4 : IpamIpVersion.IPV6;

            if (prefixLength < 0 || prefixLength > totalBits) {
                throw new IllegalArgumentException("Prefixo inválido: /" + prefixLength);
            }

            BigInteger ipNum = new BigInteger(1, bytes);
            BigInteger mask = getMask(prefixLength, totalBits);
            BigInteger wildcard = getWildcard(prefixLength, totalBits);

            BigInteger networkNum = ipNum.and(mask);
            BigInteger broadcastNum = isIpv4 ? networkNum.or(wildcard) : null;

            String networkAddress = formatIp(networkNum, isIpv4);
            String broadcastAddress = isIpv4 && broadcastNum != null ? formatIp(broadcastNum, isIpv4) : null;
            String netmask = isIpv4 ? formatIp(mask, true) : null;
            String wildcardMask = isIpv4 ? formatIp(wildcard, true) : null;

            long totalHosts;
            long usableHosts;
            String firstUsableIp;
            String lastUsableIp;

            if (isIpv4) {
                if (prefixLength == 32) {
                    totalHosts = 1;
                    usableHosts = 1;
                    firstUsableIp = networkAddress;
                    lastUsableIp = networkAddress;
                } else if (prefixLength == 31) {
                    totalHosts = 2;
                    usableHosts = 2;
                    firstUsableIp = networkAddress;
                    lastUsableIp = formatIp(networkNum.add(BigInteger.ONE), true);
                } else {
                    totalHosts = 1L << (32 - prefixLength);
                    usableHosts = Math.max(0, totalHosts - 2);
                    firstUsableIp = formatIp(networkNum.add(BigInteger.ONE), true);
                    lastUsableIp = formatIp(broadcastNum.subtract(BigInteger.ONE), true);
                }
            } else {
                // IPv6
                int hostBits = 128 - prefixLength;
                totalHosts = hostBits >= 63 ? Long.MAX_VALUE : (1L << hostBits);
                usableHosts = totalHosts;
                firstUsableIp = networkAddress;
                BigInteger lastHostNum = networkNum.or(wildcard);
                lastUsableIp = formatIp(lastHostNum, false);
            }

            return SubnetCalculationResult.builder()
                    .cidr(networkAddress + "/" + prefixLength)
                    .ipVersion(version)
                    .prefixLength(prefixLength)
                    .networkAddress(networkAddress)
                    .broadcastAddress(broadcastAddress)
                    .netmask(netmask)
                    .wildcardMask(wildcardMask)
                    .firstUsableIp(firstUsableIp)
                    .lastUsableIp(lastUsableIp)
                    .totalHosts(totalHosts)
                    .usableHosts(usableHosts)
                    .build();

        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Endereço IP inválido: " + ipPart, e);
        }
    }

    public List<SubnetCalculationResult> splitSubnet(String parentCidr, int targetPrefixLength) {
        SubnetCalculationResult parent = calculateSubnet(parentCidr);
        if (targetPrefixLength <= parent.getPrefixLength()) {
            throw new IllegalArgumentException("O prefixo alvo (/" + targetPrefixLength + ") deve ser maior que o prefixo pai (/" + parent.getPrefixLength() + ")");
        }

        boolean isIpv4 = parent.getIpVersion() == IpamIpVersion.IPV4;
        int totalBits = isIpv4 ? 32 : 128;
        if (targetPrefixLength > totalBits) {
            throw new IllegalArgumentException("Prefixo alvo excede o máximo permitido (" + totalBits + ")");
        }

        BigInteger step = BigInteger.ONE.shiftLeft(totalBits - targetPrefixLength);
        BigInteger count = BigInteger.ONE.shiftLeft(targetPrefixLength - parent.getPrefixLength());
        int maxResults = 1024;
        int limit = count.min(BigInteger.valueOf(maxResults)).intValue();

        try {
            InetAddress parentAddr = InetAddress.getByName(parent.getNetworkAddress());
            BigInteger currentNum = new BigInteger(1, parentAddr.getAddress());
            List<SubnetCalculationResult> results = new ArrayList<>(limit);

            for (int i = 0; i < limit; i++) {
                String childIp = formatIp(currentNum, isIpv4);
                results.add(calculateSubnet(childIp + "/" + targetPrefixLength));
                currentNum = currentNum.add(step);
            }

            return results;
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Erro ao processar CIDR pai: " + parentCidr, e);
        }
    }

    public boolean isOverlap(String cidrA, String cidrB) {
        SubnetCalculationResult a = calculateSubnet(cidrA);
        SubnetCalculationResult b = calculateSubnet(cidrB);
        if (a.getIpVersion() != b.getIpVersion()) {
            return false;
        }

        try {
            boolean isIpv4 = a.getIpVersion() == IpamIpVersion.IPV4;
            int totalBits = isIpv4 ? 32 : 128;

            BigInteger aStart = new BigInteger(1, InetAddress.getByName(a.getNetworkAddress()).getAddress());
            BigInteger aEnd = aStart.or(getWildcard(a.getPrefixLength(), totalBits));

            BigInteger bStart = new BigInteger(1, InetAddress.getByName(b.getNetworkAddress()).getAddress());
            BigInteger bEnd = bStart.or(getWildcard(b.getPrefixLength(), totalBits));

            return aStart.compareTo(bEnd) <= 0 && bStart.compareTo(aEnd) <= 0;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public boolean contains(String parentCidr, String childCidrOrIp) {
        SubnetCalculationResult parent = calculateSubnet(parentCidr);
        SubnetCalculationResult child = calculateSubnet(childCidrOrIp);
        if (parent.getIpVersion() != child.getIpVersion()) {
            return false;
        }

        try {
            boolean isIpv4 = parent.getIpVersion() == IpamIpVersion.IPV4;
            int totalBits = isIpv4 ? 32 : 128;

            BigInteger parentStart = new BigInteger(1, InetAddress.getByName(parent.getNetworkAddress()).getAddress());
            BigInteger parentEnd = parentStart.or(getWildcard(parent.getPrefixLength(), totalBits));

            BigInteger childStart = new BigInteger(1, InetAddress.getByName(child.getNetworkAddress()).getAddress());
            BigInteger childEnd = childStart.or(getWildcard(child.getPrefixLength(), totalBits));

            return parentStart.compareTo(childStart) <= 0 && parentEnd.compareTo(childEnd) >= 0;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    public String findNextAvailableHost(String cidr, Set<String> usedSet) {
        SubnetCalculationResult calc = calculateSubnet(cidr);
        boolean isIpv4 = calc.getIpVersion() == IpamIpVersion.IPV4;
        if (!isIpv4) {
            return calc.getFirstUsableIp();
        }

        try {
            BigInteger start = new BigInteger(1, InetAddress.getByName(calc.getFirstUsableIp()).getAddress());
            BigInteger end = new BigInteger(1, InetAddress.getByName(calc.getLastUsableIp()).getAddress());

            BigInteger current = start;
            while (current.compareTo(end) <= 0) {
                String candidate = formatIp(current, true);
                if (!usedSet.contains(candidate)) {
                    return candidate;
                }
                current = current.add(BigInteger.ONE);
            }
        } catch (UnknownHostException e) {
            // fallback
        }

        return calc.getFirstUsableIp();
    }

    private BigInteger getMask(int prefixLength, int totalBits) {
        if (prefixLength == 0) return BigInteger.ZERO;
        return BigInteger.ONE.shiftLeft(totalBits).subtract(BigInteger.ONE)
                .xor(BigInteger.ONE.shiftLeft(totalBits - prefixLength).subtract(BigInteger.ONE));
    }

    private BigInteger getWildcard(int prefixLength, int totalBits) {
        return BigInteger.ONE.shiftLeft(totalBits - prefixLength).subtract(BigInteger.ONE);
    }

    private String formatIp(BigInteger ipNum, boolean isIpv4) {
        byte[] bytes = ipNum.toByteArray();
        int targetLen = isIpv4 ? 4 : 16;
        byte[] padded = new byte[targetLen];

        if (bytes.length > targetLen) {
            System.arraycopy(bytes, bytes.length - targetLen, padded, 0, targetLen);
        } else {
            System.arraycopy(bytes, 0, padded, targetLen - bytes.length, bytes.length);
        }

        if (isIpv4) {
            return (padded[0] & 0xFF) + "." + (padded[1] & 0xFF) + "." + (padded[2] & 0xFF) + "." + (padded[3] & 0xFF);
        }

        // IPv6 RFC 5952 Canonical Formatting
        int[] hextets = new int[8];
        for (int i = 0; i < 8; i++) {
            hextets[i] = ((padded[i * 2] & 0xFF) << 8) | (padded[i * 2 + 1] & 0xFF);
        }

        // Encontra maior sequência contínua de zeros >= 2
        int maxZeroStart = -1;
        int maxZeroLen = 0;
        int currentZeroStart = -1;
        int currentZeroLen = 0;

        for (int i = 0; i < 8; i++) {
            if (hextets[i] == 0) {
                if (currentZeroStart == -1) {
                    currentZeroStart = i;
                    currentZeroLen = 1;
                } else {
                    currentZeroLen++;
                }
                if (currentZeroLen > maxZeroLen) {
                    maxZeroLen = currentZeroLen;
                    maxZeroStart = currentZeroStart;
                }
            } else {
                currentZeroStart = -1;
                currentZeroLen = 0;
            }
        }

        if (maxZeroLen < 2) {
            maxZeroStart = -1;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == maxZeroStart) {
                sb.append("::");
                i += maxZeroLen - 1;
            } else {
                if (sb.length() > 0 && !sb.toString().endsWith("::")) {
                    sb.append(":");
                }
                sb.append(Integer.toHexString(hextets[i]));
            }
        }

        return sb.toString();
    }
}
