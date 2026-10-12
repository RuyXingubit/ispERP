FROM freeradius/freeradius-server:latest

# Copia script de inicializacao dinamica
COPY docker/freeradius/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Portas padrao RADIUS: Auth (1812), Acct (1813) e CoA/PoD (3799)
EXPOSE 1812/udp 1813/udp 3799/udp

ENTRYPOINT ["/entrypoint.sh"]
CMD ["freeradius", "-f", "-l", "stdout"]
