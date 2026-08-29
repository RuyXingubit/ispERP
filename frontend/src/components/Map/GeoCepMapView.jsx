import React, { useEffect, useRef } from 'react';
import * as maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { Box, Typography } from '@mui/material';

const GeoCepMapView = ({
  technicianLocation = null,
  customerLocation = null,
  height = '280px',
  zoom = 15,
}) => {
  const mapContainer = useRef(null);
  const mapInstance = useRef(null);
  const techMarkerRef = useRef(null);
  const custMarkerRef = useRef(null);

  useEffect(() => {
    if (!mapContainer.current) return;

    // Ponto central padrão: Altamira/PA ou cliente ou técnico
    const defaultCenter = customerLocation
      ? [customerLocation.longitude, customerLocation.latitude]
      : technicianLocation
      ? [technicianLocation.longitude, technicianLocation.latitude]
      : [-52.2064, -3.2033]; // Altamira - PA

    try {
      const map = new maplibregl.Map({
        container: mapContainer.current,
        style: 'https://geocep.api.br/v1/maps/style.json',
        center: defaultCenter,
        zoom: zoom,
        attributionControl: false,
      });

      map.addControl(new maplibregl.NavigationControl({ showCompass: true }), 'top-right');
      mapInstance.current = map;

      map.on('load', () => {
        updateMarkers(map);
      });

      return () => {
        map.remove();
        mapInstance.current = null;
      };
    } catch (e) {
      console.warn('Erro ao inicializar mapa MapLibre GeoCEP:', e);
    }
  }, []);

  useEffect(() => {
    if (mapInstance.current && mapInstance.current.loaded()) {
      updateMarkers(mapInstance.current);
    }
  }, [technicianLocation, customerLocation]);

  const updateMarkers = (map) => {
    // Marcador do Cliente (Vermelho)
    if (customerLocation && customerLocation.latitude && customerLocation.longitude) {
      if (custMarkerRef.current) custMarkerRef.current.remove();

      const el = document.createElement('div');
      el.className = 'geocep-customer-marker';
      el.style.width = '24px';
      el.style.height = '24px';
      el.style.borderRadius = '50%';
      el.style.backgroundColor = '#dc2626';
      el.style.border = '3px solid #ffffff';
      el.style.boxShadow = '0 2px 6px rgba(0,0,0,0.3)';

      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(`
        <div style="font-family: sans-serif; font-size: 12px; color: #1e293b;">
          <strong>🏠 Destino / Cliente</strong><br/>
          <span>${customerLocation.label || 'Instalação de Fibra'}</span>
        </div>
      `);

      custMarkerRef.current = new maplibregl.Marker({ element: el })
        .setLngLat([customerLocation.longitude, customerLocation.latitude])
        .setPopup(popup)
        .addTo(map);
    }

    // Marcador do Técnico (Azul com pulso)
    if (technicianLocation && technicianLocation.latitude && technicianLocation.longitude) {
      if (techMarkerRef.current) techMarkerRef.current.remove();

      const el = document.createElement('div');
      el.className = 'geocep-tech-marker';
      el.style.width = '20px';
      el.style.height = '20px';
      el.style.borderRadius = '50%';
      el.style.backgroundColor = '#0284c7';
      el.style.border = '3px solid #ffffff';
      el.style.boxShadow = '0 0 10px rgba(2, 132, 199, 0.8)';

      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(`
        <div style="font-family: sans-serif; font-size: 12px; color: #1e293b;">
          <strong>📍 Sua Posição (Técnico)</strong>
        </div>
      `);

      techMarkerRef.current = new maplibregl.Marker({ element: el })
        .setLngLat([technicianLocation.longitude, technicianLocation.latitude])
        .setPopup(popup)
        .addTo(map);
    }

    // Ajusta o enquadramento (Fit Bounds) se tiver ambos os pontos
    if (
      customerLocation &&
      customerLocation.latitude &&
      technicianLocation &&
      technicianLocation.latitude
    ) {
      const bounds = new maplibregl.LngLatBounds()
        .extend([customerLocation.longitude, customerLocation.latitude])
        .extend([technicianLocation.longitude, technicianLocation.latitude]);

      map.fitBounds(bounds, { padding: 40, maxZoom: 16 });
    } else if (customerLocation && customerLocation.latitude) {
      map.flyTo({ center: [customerLocation.longitude, customerLocation.latitude], zoom: 15 });
    }
  };

  return (
    <Box sx={{ position: 'relative', width: '100%', height, borderRadius: 2, overflow: 'hidden', border: '1px solid', borderColor: 'grey.300' }}>
      <div ref={mapContainer} style={{ width: '100%', height: '100%' }} />
      <Box
        sx={{
          position: 'absolute',
          bottom: 4,
          left: 6,
          bgcolor: 'rgba(255, 255, 255, 0.85)',
          backdropFilter: 'blur(4px)',
          px: 1,
          py: 0.3,
          borderRadius: 1,
          border: '1px solid',
          borderColor: 'grey.300',
        }}
      >
        <Typography variant="caption" sx={{ fontSize: '10px', fontWeight: 'bold', color: 'primary.main' }}>
          🗺️ GeoCEP Vector Maps
        </Typography>
      </Box>
    </Box>
  );
};

export default GeoCepMapView;
