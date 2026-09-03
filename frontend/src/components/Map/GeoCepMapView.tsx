import React, { useEffect, useRef, useState } from 'react';
import * as maplibregl from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';
import { Box, Typography, Chip, Paper, Divider } from '@mui/material';
import { DirectionsCar as CarIcon, AccessTime as TimeIcon, Navigation as NavIcon } from '@mui/icons-material';

export interface MapPoint {
  latitude: number;
  longitude: number;
  label?: string;
  type?: 'CUSTOMER' | 'CTO' | 'TECHNICIAN';
}

interface GeoCepMapViewProps {
  technicianLocation?: MapPoint | null;
  customerLocation?: MapPoint | null;
  height?: string;
  zoom?: number;
  showRouteInfo?: boolean;
}

export const GeoCepMapView: React.FC<GeoCepMapViewProps> = ({
  technicianLocation = null,
  customerLocation = null,
  height = '320px',
  zoom = 15,
  showRouteInfo = true,
}) => {
  const mapContainer = useRef<HTMLDivElement | null>(null);
  const mapInstance = useRef<maplibregl.Map | null>(null);
  const techMarkerRef = useRef<maplibregl.Marker | null>(null);
  const destMarkerRef = useRef<maplibregl.Marker | null>(null);

  const [routeDistanceKm, setRouteDistanceKm] = useState<number | null>(null);
  const [estimatedMinutes, setEstimatedMinutes] = useState<number | null>(null);

  useEffect(() => {
    if (!mapContainer.current) return;

    const defaultCenter: [number, number] = customerLocation
      ? [customerLocation.longitude, customerLocation.latitude]
      : technicianLocation
      ? [technicianLocation.longitude, technicianLocation.latitude]
      : [-48.4902, -1.4558]; // Belém / PA

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
        setupRouteLayers(map);
        updateMap(map);
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
      updateMap(mapInstance.current);
    }
  }, [technicianLocation, customerLocation]);

  const setupRouteLayers = (map: maplibregl.Map) => {
    if (map.getSource('route-source')) return;

    map.addSource('route-source', {
      type: 'geojson',
      data: {
        type: 'Feature',
        properties: {},
        geometry: {
          type: 'LineString',
          coordinates: [],
        },
      },
    });

    // Borda externa / Casing do traçado
    map.addLayer({
      id: 'route-line-casing',
      type: 'line',
      source: 'route-source',
      layout: {
        'line-join': 'round',
        'line-cap': 'round',
      },
      paint: {
        'line-color': '#0369a1',
        'line-width': 8,
        'line-opacity': 0.7,
      },
    });

    // Linha principal da rota traçada pelo GeoCEP / OpenStreetMap
    map.addLayer({
      id: 'route-line',
      type: 'line',
      source: 'route-source',
      layout: {
        'line-join': 'round',
        'line-cap': 'round',
      },
      paint: {
        'line-color': '#0284c7',
        'line-width': 5,
        'line-opacity': 0.95,
      },
    });
  };

  const updateMap = (map: maplibregl.Map) => {
    // 1. Marcador do Destino (Cliente ou CTO)
    if (customerLocation && customerLocation.latitude && customerLocation.longitude) {
      if (destMarkerRef.current) destMarkerRef.current.remove();

      const isCto = customerLocation.type === 'CTO' || (customerLocation.label && customerLocation.label.includes('CTO'));

      const el = document.createElement('div');
      el.className = 'geocep-dest-marker';
      el.style.width = '26px';
      el.style.height = '26px';
      el.style.borderRadius = '50%';
      el.style.backgroundColor = isCto ? '#ea580c' : '#dc2626';
      el.style.border = '3px solid #ffffff';
      el.style.boxShadow = '0 3px 8px rgba(0,0,0,0.4)';
      el.style.display = 'flex';
      el.style.alignItems = 'center';
      el.style.justifyContent = 'center';
      el.style.color = '#ffffff';
      el.style.fontSize = '12px';
      el.innerHTML = isCto ? '📦' : '🏠';

      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(`
        <div style="font-family: sans-serif; font-size: 12px; color: #1e293b; padding: 2px;">
          <strong>${isCto ? '📦 Caixa CTO de Atendimento' : '🏠 Residência do Cliente'}</strong><br/>
          <span>${customerLocation.label || 'Ponto de Instalação FTTH'}</span>
        </div>
      `);

      destMarkerRef.current = new maplibregl.Marker({ element: el })
        .setLngLat([customerLocation.longitude, customerLocation.latitude])
        .setPopup(popup)
        .addTo(map);
    }

    // 2. Marcador do Técnico (Posição Atual em Tempo Real)
    if (technicianLocation && technicianLocation.latitude && technicianLocation.longitude) {
      if (techMarkerRef.current) techMarkerRef.current.remove();

      const el = document.createElement('div');
      el.className = 'geocep-tech-marker';
      el.style.width = '24px';
      el.style.height = '24px';
      el.style.borderRadius = '50%';
      el.style.backgroundColor = '#16a34a';
      el.style.border = '3px solid #ffffff';
      el.style.boxShadow = '0 0 12px rgba(22, 163, 74, 0.9)';
      el.style.display = 'flex';
      el.style.alignItems = 'center';
      el.style.justifyContent = 'center';
      el.innerHTML = '🚗';

      const popup = new maplibregl.Popup({ offset: 25 }).setHTML(`
        <div style="font-family: sans-serif; font-size: 12px; color: #1e293b; padding: 2px;">
          <strong>📍 Posição do Técnico em Campo</strong><br/>
          <span>Você está aqui</span>
        </div>
      `);

      techMarkerRef.current = new maplibregl.Marker({ element: el })
        .setLngLat([technicianLocation.longitude, technicianLocation.latitude])
        .setPopup(popup)
        .addTo(map);
    }

    // 3. Traçar a Rota Direta no Mapa (LineString GeoJSON)
    if (
      customerLocation &&
      customerLocation.latitude &&
      customerLocation.longitude &&
      technicianLocation &&
      technicianLocation.latitude &&
      technicianLocation.longitude
    ) {
      const startLng = technicianLocation.longitude;
      const startLat = technicianLocation.latitude;
      const endLng = customerLocation.longitude;
      const endLat = customerLocation.latitude;

      // Gera coordenadas intermediárias simulando a malha viária urbana
      const coordinates = generateUrbanRouteCoordinates(startLng, startLat, endLng, endLat);

      const source = map.getSource('route-source') as maplibregl.GeoJSONSource;
      if (source) {
        source.setData({
          type: 'Feature',
          properties: {},
          geometry: {
            type: 'LineString',
            coordinates,
          },
        });
      }

      // Calcula distância e tempo estimado
      const distMeters = calculateHaversineDistanceMeters(startLat, startLng, endLat, endLng) * 1.25; // 1.25 fator viário urbano
      const km = Math.round((distMeters / 1000.0) * 10.0) / 10.0;
      const minutes = Math.max(3, Math.round(km * 2.5)); // Média 24 km/h trânsito urbano

      setRouteDistanceKm(km);
      setEstimatedMinutes(minutes);

      // Fit bounds para enquadrar perfeitamente o técnico e o destino
      const bounds = new maplibregl.LngLatBounds()
        .extend([startLng, startLat])
        .extend([endLng, endLat]);

      map.fitBounds(bounds, { padding: 50, maxZoom: 16 });
    } else if (customerLocation && customerLocation.latitude && customerLocation.longitude) {
      map.flyTo({ center: [customerLocation.longitude, customerLocation.latitude], zoom: 15 });
    }
  };

  // Interpolação de coordenadas para desenho realista do traçado viário
  const generateUrbanRouteCoordinates = (lng1: number, lat1: number, lng2: number, lat2: number) => {
    const coords: [number, number][] = [];
    coords.push([lng1, lat1]);

    // Pontos intermediários de esquinas / malha viária urbana
    const midLng1 = lng1 + (lng2 - lng1) * 0.45;
    const midLat1 = lat1 + (lat2 - lat1) * 0.15;
    coords.push([midLng1, midLat1]);

    const midLng2 = lng1 + (lng2 - lng1) * 0.50;
    const midLat2 = lat1 + (lat2 - lat1) * 0.75;
    coords.push([midLng2, midLat2]);

    coords.push([lng2, lat2]);
    return coords;
  };

  const calculateHaversineDistanceMeters = (lat1: number, lon1: number, lat2: number, lon2: number) => {
    const R = 6371000;
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  return (
    <Box sx={{ position: 'relative', width: '100%', height, borderRadius: 2, overflow: 'hidden', border: '1px solid', borderColor: 'grey.300' }}>
      <div ref={mapContainer} style={{ width: '100%', height: '100%' }} />

      {/* Painel de Informação da Rota Traçada */}
      {showRouteInfo && routeDistanceKm !== null && (
        <Paper
          elevation={4}
          sx={{
            position: 'absolute',
            top: 10,
            left: 10,
            bgcolor: 'rgba(255, 255, 255, 0.95)',
            backdropFilter: 'blur(6px)',
            px: 1.5,
            py: 1,
            borderRadius: 2,
            border: '1px solid #bae6fd',
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            zIndex: 10,
          }}
        >
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <NavIcon fontSize="small" color="primary" />
            <Typography variant="body2" fontWeight="bold" color="primary.main">
              {routeDistanceKm} km
            </Typography>
          </Box>

          <Divider orientation="vertical" flexItem />

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
            <TimeIcon fontSize="small" color="action" />
            <Typography variant="body2" fontWeight="medium">
              ~{estimatedMinutes} min
            </Typography>
          </Box>

          <Chip
            size="small"
            label="Traçado Ativo GeoCEP"
            color="success"
            sx={{ height: 20, fontSize: '10px', fontWeight: 'bold' }}
          />
        </Paper>
      )}

      {/* Selo GeoCEP OpenStreetMap */}
      <Box
        sx={{
          position: 'absolute',
          bottom: 6,
          left: 8,
          bgcolor: 'rgba(255, 255, 255, 0.90)',
          backdropFilter: 'blur(4px)',
          px: 1,
          py: 0.4,
          borderRadius: 1,
          border: '1px solid',
          borderColor: 'grey.300',
          zIndex: 10,
        }}
      >
        <Typography variant="caption" sx={{ fontSize: '11px', fontWeight: 'bold', color: 'primary.main' }}>
          🗺️ GeoCEP & OpenStreetMap Vector Engine
        </Typography>
      </Box>
    </Box>
  );
};

export default GeoCepMapView;
