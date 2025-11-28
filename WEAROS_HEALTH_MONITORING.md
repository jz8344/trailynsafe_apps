# 🩺 Sistema de Monitoreo de Signos Vitales Wear OS - TrailynSafe

## 📋 Descripción General

Sistema completo de monitoreo de signos vitales del chofer mediante smartwatch Wear OS durante los viajes escolares. Incluye validación obligatoria de conexión antes de iniciar cualquier viaje.

---

## 🏗️ Arquitectura del Sistema

### **1. App Wear OS (Smartwatch)**
- **Ubicación**: `appwearos/`
- **Sensores**: Frecuencia cardíaca (BODY_SENSORS), contador de pasos
- **Funcionalidad**: 
  - Lectura continua de sensores corporales
  - Sincronización automática cada 10 segundos
  - Interfaz Jetpack Compose con visualización en tiempo real
  - Servicio en segundo plano para sincronización persistente

### **2. App Driver (Teléfono del Chofer)**
- **Ubicación**: `appdriver/`
- **Funcionalidad**:
  - Recepción de datos vía Wearable Data Layer API
  - Validación de conexión Wear OS antes de iniciar viajes
  - Dashboard en tiempo real durante navegación
  - Alertas automáticas de taquicardia (>120 BPM)
  - Diálogo completo de signos vitales

### **3. Comunicación Bluetooth/Nodes**
- **Protocolo**: Google Wearable Data Layer API
- **Path**: `/health_data`
- **Datos transmitidos**:
  ```kotlin
  {
    "heart_rate": Int,        // BPM
    "steps": Int,             // Pasos totales
    "timestamp": Long,        // Milisegundos
    "status": String          // "Normal", "Elevado", "Taquicardia", etc.
  }
  ```

---

## 🚀 Configuración y Uso

### **Paso 1: Emparejar Dispositivos**

1. **En el emulador Wear OS Large Round AVD**:
   - Iniciar emulador Wear OS
   - Ir a Settings > Connectivity > Bluetooth
   - Activar Bluetooth

2. **En el celular físico**:
   - Instalar "Wear OS by Google" desde Play Store
   - Abrir app y seguir pasos de emparejamiento
   - Buscar "Android Wear..." en dispositivos disponibles
   - Completar emparejamiento

3. **Verificar conexión**:
   ```bash
   adb -s emulator-5554 forward tcp:5601 tcp:5601
   ```

### **Paso 2: Instalar Apps**

1. **Instalar app Wear OS en smartwatch**:
   ```bash
   cd TrailynApp
   ./gradlew :appwearos:installDebug
   ```

2. **Instalar app Driver en celular**:
   ```bash
   ./gradlew :appdriver:installDebug
   ```

### **Paso 3: Iniciar Monitoreo**

1. **En el smartwatch**:
   - Abrir app "TrailynSafe"
   - Conceder permisos de BODY_SENSORS
   - El monitoreo iniciará automáticamente
   - Ver: ❤️ Frecuencia cardíaca, 👣 Pasos

2. **En el celular (app driver)**:
   - Los datos se sincronizarán automáticamente
   - Verificar en cualquier viaje: botón "⌚ Ver Signos Vitales"

### **Paso 4: Validación Pre-Viaje**

**Flujo automático**:
1. Chofer abre `TripDetailActivity`
2. Al presionar "Comenzar Viaje" o "Abrir Navegación":
   - Sistema valida conexión Wear OS
   - Si **NO está conectado**: Muestra diálogo de advertencia obligatorio
   - Si **está conectado**: Permite iniciar viaje

3. Durante navegación:
   - Visualización en tiempo real en panel inferior
   - Actualización cada 3 segundos
   - Alertas automáticas si FC > 120 BPM

---

## 📱 Características Implementadas

### **App Wear OS**

✅ **Sensores Virtuales del Emulador**:
- Frecuencia cardíaca: 50-200 BPM (ajustable en emulador)
- Contador de pasos (acelerómetro virtual)
- Batería del dispositivo

✅ **Interfaz Compose**:
```kotlin
@Composable
fun HealthMonitorScreen(...)
  ├── Card Frecuencia Cardíaca
  │   ├── Icono ❤️
  │   ├── Valor BPM (grande, color dinámico)
  │   └── Estado: Normal/Elevado/Taquicardia
  │
  ├── Card Pasos
  │   ├── Icono 👣
  │   └── Contador total
  │
  └── Botón Toggle Monitoreo
```

✅ **Servicio de Sincronización**:
```kotlin
class HealthDataSyncService : Service()
  - Lectura continua de sensores
  - Envío cada 10 segundos
  - Logging detallado
```

### **App Driver**

✅ **Recepción de Datos**:
```kotlin
class WearableDataListenerService : WearableListenerService()
  - Listener automático del Wearable Data Layer
  - Guardado en SharedPreferences
  - Broadcast a actividades activas
```

✅ **Validación Pre-Viaje**:
```kotlin
private fun handleTripAction() {
    if (viaje.estado == "ruta_generada" || "en_curso") {
        if (!WearOSHealthManager.isConnected(this)) {
            showWearOSRequiredDialog() // ⚠️ OBLIGATORIO
            return
        }
    }
    // Continuar con inicio de viaje...
}
```

✅ **Dashboard en Navegación**:
- Panel superior con FC en tiempo real
- Color dinámico según estado
- FAB para abrir diálogo completo
- Actualización cada 3 segundos

✅ **Diálogo Completo**:
```kotlin
class WearOSHealthDialog : DialogFragment()
  ├── Estado de conexión
  ├── Última actualización (timestamp)
  ├── Card Frecuencia Cardíaca
  ├── Card Pasos
  ├── Botón Reintentar (si desconectado)
  └── Auto-refresh cada 2 segundos
```

---

## 🎯 Estados de Salud

| BPM | Estado | Color | Acción |
|-----|--------|-------|--------|
| 0 | Sin señal | Gris | Verificar conexión |
| <60 | Bradicardia | Azul | Monitorear |
| 60-100 | **Normal** | Verde | ✓ OK |
| 101-120 | Elevado | Amarillo | Atención |
| >120 | ⚠️ Taquicardia | Rojo | **ALERTA** |

---

## 🔧 Troubleshooting

### **Problema: Datos no se sincronizan**

1. Verificar emparejamiento Bluetooth:
   ```bash
   adb devices
   adb -s emulator-5554 shell dumpsys bluetooth_manager
   ```

2. Verificar permisos en Wear OS:
   - BODY_SENSORS
   - WAKE_LOCK

3. Verificar logs:
   ```bash
   # Wear OS
   adb -s emulator-5554 logcat | grep HealthDataSync
   
   # App Driver
   adb logcat | grep WearableDataListener
   ```

### **Problema: Sensores virtuales no responden**

1. En el emulador Wear OS:
   - Extended Controls (⋮)
   - Virtual Sensors
   - Heart Rate: Ajustar valor manualmente
   - Accelerometer: Simular movimiento

2. Reiniciar servicio:
   ```kotlin
   // Forzar restart del servicio
   stopService(Intent(this, HealthDataSyncService::class.java))
   startService(Intent(this, HealthDataSyncService::class.java))
   ```

### **Problema: Conexión se pierde constantemente**

- Timeout: 30 segundos sin datos = desconectado
- Verificar que la app Wear OS esté en foreground
- Evitar cerrar app desde recents

---

## 📊 Logs de Depuración

### **Wear OS (Envío)**
```
D/HealthDataSyncService: ✓ Datos enviados: HR=72, Steps=1523
D/HealthDataSyncService: ❤️ Frecuencia cardíaca: 72 BPM
D/HealthDataSyncService: 👣 Pasos: 1523
```

### **App Driver (Recepción)**
```
D/WearableDataListener: 📊 Datos recibidos del smartwatch:
D/WearableDataListener:    ❤️ Frecuencia cardíaca: 72 BPM
D/WearableDataListener:    👣 Pasos: 1523
D/WearableDataListener:    📍 Estado: Normal
D/WearableDataListener:    🕐 Timestamp: 1701234567890
```

---

## 🎨 Diseño UI

### **Wear OS (Round Screen)**
```
┌─────────────────┐
│   ⌚ 10:45      │
│                 │
│      ❤️        │
│      72        │  (Grande, color verde)
│      BPM       │
│    Normal      │
│                 │
│      👣        │
│     1,523      │
│    Pasos       │
│                 │
│  [Detener]     │
└─────────────────┘
```

### **App Driver (Durante Navegación)**
```
┌──────────────────────────────┐
│ 🗺️ MAPA GOOGLE MAPS         │
│                              │
│ ╔════════════════════════╗  │
│ ║ Ruta Viaje #10         ║  │
│ ║ 6.81 km | 20 min       ║  │
│ ╚════════════════════════╝  │
│                              │
│                         [📍] │ FAB Mi Ubicación
│                         [➡️] │ FAB Recentrar
│                         [⌚] │ FAB Signos Vitales
│                              │
│ ╔════════════════════════╗  │
│ ║ ❤️ 72 BPM | Normal    ║  │  ← NUEVO
│ ║─────────────────────────║  │
│ ║ 📍 Siguiente Parada     ║  │
│ ║ C. Flaviano Ramos #33   ║  │
│ ║ ⏰ 10:03 | 📏 3.45 km   ║  │
│ ║                         ║  │
│ ║  [✓ Completar Parada]  ║  │
│ ╚════════════════════════╝  │
└──────────────────────────────┘
```

---

## 📦 Archivos Creados/Modificados

### **Nuevos Archivos**

```
appwearos/
├── src/main/java/.../presentation/
│   └── HealthMonitorScreen.kt          ✨ NUEVO
├── src/main/java/.../services/
│   └── HealthDataSyncService.kt        ✨ NUEVO

appdriver/
├── src/main/java/.../services/
│   └── WearableDataListenerService.kt  ✨ NUEVO
├── src/main/java/.../utils/
│   └── WearOSHealthManager.kt          ✨ NUEVO
├── src/main/java/.../ui/wearos/
│   └── WearOSHealthDialog.kt           ✨ NUEVO
├── src/main/res/layout/
│   └── dialog_wearos_health.xml        ✨ NUEVO
```

### **Archivos Modificados**

```
appwearos/
├── src/main/java/.../presentation/MainActivity.kt      ✏️ MODIFICADO
├── src/main/AndroidManifest.xml                        ✏️ MODIFICADO

appdriver/
├── src/main/java/.../ui/trips/TripDetailActivity.kt    ✏️ MODIFICADO
├── src/main/java/.../ui/navigation/NavigationActivity.kt ✏️ MODIFICADO
├── src/main/res/layout/activity_trip_detail.xml        ✏️ MODIFICADO
├── src/main/res/layout/activity_navigation.xml         ✏️ MODIFICADO
├── src/main/AndroidManifest.xml                        ✏️ MODIFICADO
├── build.gradle.kts                                    ✏️ MODIFICADO
```

---

## ✅ Checklist de Implementación

- [x] App Wear OS con lectura de sensores
- [x] Servicio de sincronización en background
- [x] Comunicación Bluetooth/Wearable Data Layer
- [x] Recepción de datos en app driver
- [x] Validación obligatoria pre-viaje
- [x] Dashboard en tiempo real durante navegación
- [x] Diálogo completo de signos vitales
- [x] Alertas de taquicardia
- [x] Manejo de desconexiones
- [x] UI responsive y Material Design
- [x] Permisos y manifiestos configurados
- [x] Logs de depuración completos

---

## 🎓 Próximos Pasos (Opcional)

1. **Almacenamiento en Backend**:
   - Enviar datos al servidor Laravel/Django
   - Historial de signos vitales por viaje
   - Análisis de tendencias

2. **Alertas Avanzadas**:
   - Notificaciones push al administrador
   - Pausa automática de viaje si FC crítica
   - Sugerencias de descanso

3. **Sensores Adicionales**:
   - SpO2 (saturación de oxígeno)
   - Temperatura corporal
   - Nivel de estrés

4. **Machine Learning**:
   - Predicción de fatiga del conductor
   - Detección de anomalías
   - Recomendaciones personalizadas

---

## 📞 Soporte

Para problemas o dudas:
- Revisar logs con `adb logcat`
- Verificar permisos en ambas apps
- Asegurar emparejamiento Bluetooth correcto
- Reiniciar servicios si es necesario

---

**¡Sistema de monitoreo Wear OS completamente funcional! 🩺⌚**
