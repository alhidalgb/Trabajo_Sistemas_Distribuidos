# Ruleta de Ganancias

Sistema cliente-servidor de ruleta europea con gestión concurrente de apuestas.

---

## Descripción

Aplicación multijugador que simula una ruleta de casino. Permite conexiones simultáneas de clientes. Gestiona sesiones de usuario, apuestas en tiempo real y cálculo automático de premios. Persistencia de datos en formato XML.

### Características principales

- Ruleta europea estándar con números 0-36
- Soporte para múltiples jugadores concurrentes
- Tipos de apuesta: número pleno, color, paridad, docena
- Sincronización de estados de mesa
- Persistencia de usuarios y historial de partidas
- Arquitectura basada en pools de hilos
- Sistema de login y registro

---

## Arquitectura

### Modelo Cliente-Servidor

```
┌─────────────┐         TCP/IP         ┌─────────────┐
│   CLIENTE   │◄─────────────────────► │   SERVIDOR  │
│             │   ObjectStreams        │             │
│ - Interfaz  │                        │ - Lógica    │
│ - Apuestas  │                        │ - Ruleta    │
│ - Listener  │                        │ - Workers   │
└─────────────┘                        └─────────────┘
```

### Componentes del Servidor

**Servidor Principal (ServidorRuleta)**
- Acepta conexiones TCP en puerto 8000
- Carga estado inicial desde XML
- Programa tareas periódicas
- Gestiona pools de hilos

**Servicio de Ruleta (ServicioRuleta)**
- Mantiene lista de jugadores conectados
- Gestiona mapa de apuestas activas
- Controla estado de mesa (abierta/cerrada)
- Coordina broadcast de mensajes
- Calcula y reparte premios

**Ciclo de Juego (GiraPelotita)**
- Cierra apuestas (NO VA MAS)
- Genera número ganador aleatorio
- Comunica resultado a todos los clientes
- Reparte premios
- Abre nueva ronda

**Atención de Cliente (AtenderJugador)**
- Gestiona comunicación con un cliente
- Procesa login/registro
- Recibe y valida apuestas
- Envía confirmaciones

**Tareas Concurrentes**
- `mandarMensaje`: broadcast paralelo a jugadores
- `mandarPremios`: cálculo distribuido de ganancias
- `getIDHilos`: búsqueda paralela de usuarios
- `cancelarFuture`: cancelación de tareas pendientes

### Componentes del Cliente

**Cliente Principal (ClienteRuleta)**
- Conecta con servidor
- Gestiona menú de usuario
- Valida apuestas localmente
- Sincroniza con estado de mesa

**Listener (mostrarYLeerServidor)**
- Hilo dedicado a recibir mensajes
- Actualiza saldo local
- Controla barreras de sincronización
- Muestra notificaciones del servidor

---

## Estructura del Proyecto

```
src/
├── cliente/
│   ├── ClienteRuleta.java           # Controlador principal cliente
│   └── mostrarYLeerServidor.java    # Listener asíncrono
│
├── logicaRuleta/
│   ├── core/
│   │   ├── ServicioRuleta.java      # Gestor central del juego
│   │   ├── AtenderJugador.java      # Worker por cliente
│   │   └── RuletaUtils.java         # Utilidades de cálculo
│   │
│   └── concurrencia/
│       ├── mandarMensaje.java       # Broadcast paralelo
│       ├── mandarPremios.java       # Reparto de premios
│       ├── getIDHilos.java          # Búsqueda concurrente
│       └── cancelarFuture.java      # Cancelación de tareas
│
├── modeloDominio/
│   ├── Jugador.java                 # Entidad usuario
│   ├── Apuesta.java                 # Transacción de juego
│   ├── Casilla.java                 # Celda de ruleta
│   ├── TipoApuesta.java             # Enum de modalidades
│   └── ListaJugadores.java          # Wrapper JAXB
│
└── servidor/
    ├── red/
    │   ├── ServidorRuleta.java      # Entry point servidor
    │   ├── GiraPelotita.java        # Orquestador de ronda
    │   └── guardarApuestas.java     # Persistencia historial
    │
    └── persistencia/
        ├── BDJugadores.java         # Marshalling JAXB
        ├── XMLServidor.java         # Gestor historial XML
        └── ActualizarBD.java        # Tarea guardado periódico
```

---

## Modelo de Dominio

### Jugador
- ID único
- Saldo en euros
- Stream de salida (conexión TCP)
- Estado de sesión

### Apuesta
- Jugador asociado
- Tipo (NUMERO, COLOR, PAR_IMPAR, DOCENA)
- Valor apostado (string)
- Cantidad en euros

### Casilla
- Número (0-36)
- Color (VERDE, ROJO, NEGRO)
- Docena calculada (1, 2, 3)

---

## Flujo de Juego

### Ciclo de una Ronda

1. **Apertura de Mesa** (20 segundos)
   - Clientes pueden realizar apuestas
   - Servidor valida saldo suficiente
   - Apuestas se registran en mapa concurrente

2. **Cierre de Apuestas**
   - Servidor envía "NO VA MAS"
   - Clientes se bloquean (CountDownLatch)
   - No se aceptan más apuestas

3. **Giro** (3 segundos)
   - Generación de número aleatorio 0-36
   - Creación de objeto Casilla

4. **Comunicación de Resultado**
   - Broadcast del número ganador
   - Todos los clientes reciben información

5. **Cálculo de Premios**
   - Tarea por jugador en paralelo
   - Aplicación de multiplicadores según tipo
   - Actualización de saldos

6. **Persistencia**
   - Guardado asíncrono en historial XML
   - Backup periódico de usuarios

7. **Cooldown** (4 segundos)
   - Tiempo para leer resultados
   - Preparación de nueva ronda

---

## Multiplicadores de Premio

| Tipo Apuesta | Multiplicador | Ejemplo |
|-------------|---------------|---------|
| Número (0-36) | x36 | 10€ → 360€ |
| Número 0 | x300 | 10€ → 3000€ |
| Color (Rojo/Negro) | x2 | 10€ → 20€ |
| Paridad (Par/Impar) | x2 | 10€ → 20€ |
| Docena (1-12, 13-24, 25-36) | x3 | 10€ → 30€ |

### Reglas Especiales

- El 0 no cuenta para color, paridad ni docena
- Solo da premio en apuesta directa a número
- Impar/Par: 0 siempre pierde

---

## Persistencia

### Base de Datos de Jugadores (jugadores.xml)

Formato JAXB con marshalling/unmarshalling automático.

**DTD (jugadores.dtd):**
```dtd
<?xml version="1.0" encoding="UTF-8"?>

<!ELEMENT jugadores (jugador*)>
<!ELEMENT jugador (saldo)>
<!ATTLIST jugador
  id ID #REQUIRED
>
<!ELEMENT saldo (#PCDATA)>
```

**Ejemplo de XML:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jugadores>
    <jugador id="usuario1">
        <saldo>1500.0</saldo>
    </jugador>
    <jugador id="usuario2">
        <saldo>850.5</saldo>
    </jugador>
</jugadores>
```

Guardado automático cada minuto y al desconectar jugador.

### Historial de Apuestas (historial.xml)

Registro de cada ronda con DOM parser.

**DTD (historial.dtd):**
```dtd
<?xml version="1.0" encoding="UTF-8"?>

<!ELEMENT historial (listapuestas*)>
<!ELEMENT listapuestas (jugador*)>

<!ATTLIST listapuestas
  fecha CDATA #REQUIRED
  ganadora CDATA #REQUIRED
>

<!ELEMENT jugador (apuesta+)>

<!ATTLIST jugador
  id CDATA #REQUIRED
>

<!ELEMENT apuesta EMPTY>

<!ATTLIST apuesta
  tipo (NUMERO|COLOR|PAR_IMPAR|DOCENA) #REQUIRED
  valor CDATA #REQUIRED
  cantidad CDATA #REQUIRED
>
```

**Ejemplo de XML:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<historial>
    <listapuestas fecha="Wed Dec 10 15:30:45 CET 2025" 
                  ganadora="Casilla [23 | ROJO]">
        <jugador id="usuario1">
            <apuesta tipo="COLOR" valor="ROJO" cantidad="50.0"/>
            <apuesta tipo="NUMERO" valor="23" cantidad="10.0"/>
        </jugador>
    </listapuestas>
</historial>
```

---

## Concurrencia y Sincronización

### Mecanismos Utilizados

**ExecutorService (CachedThreadPool)**
- Pool escalable para conexiones
- Creación bajo demanda de hilos
- Reutilización de workers inactivos

**ScheduledExecutorService**
- Programación de rondas cada 20 segundos
- Guardado periódico cada minuto

**CountDownLatch**
- Sincronización de broadcasts
- Espera de finalización de premios
- Coordinación cliente-servidor

**CyclicBarrier**
- Sincronización de envío paralelo
- Garantiza que todos reciben mensaje antes de continuar

**synchronized**
- Protección de escrituras en jugador
- Acceso exclusivo a streams
- Sincronización de colecciones críticas

**volatile**
- Variable `isNoVaMas` visible entre hilos
- Lectura sin bloqueo del estado de mesa

**Collections.synchronizedList**
- Listas thread-safe básicas
- Protección contra corrupción estructural

**ConcurrentHashMap**
- Mapa de apuestas sin bloqueo global
- Alto rendimiento en escrituras concurrentes

### Prevención de Deadlocks

- Orden consistente de adquisición de locks
- Uso de timeouts en operaciones bloqueantes
- Liberación de recursos en bloques finally

---

## Protocolo de Comunicación

### Mensajes Cliente → Servidor

**Handshake**
- `"1"`: Iniciar sesión
- `"2"`: Registrarse

**Operaciones**
- `Apuesta`: Objeto serializado con apuesta
- `Double`: Recarga de saldo
- `"SALIR"`: Desconexión ordenada

### Mensajes Servidor → Cliente

**Control de Mesa**
- `"NO_VA_MAS"`: Cierre de apuestas
- `"ABRIR MESA"`: Inicio de nueva ronda

**Actualizaciones**
- `"actualizar saldo:X"`: Modificación de saldo local
- `String`: Mensajes informativos y resultados

**Estado Inicial**
- `Boolean`: Estado actual de mesa al conectar
- `Jugador`: Confirmación de login/registro

---

## Requisitos

### Software

- Java JDK 8 o superior
- JAXB (incluido en JDK 8, módulo externo en JDK 9+)

### Hardware Mínimo

- 512 MB RAM
- Conexión de red
- Puerto 8000 disponible (servidor)

---

## Instalación y Ejecución

### Compilación

```bash
cd RuletaDeGanancias_AlbertoHidalgoBenito/src
javac -d ../bin servidor/red/ServidorRuleta.java
javac -d ../bin cliente/ClienteRuleta.java
```

### Ejecución del Servidor

```bash
cd ../bin
java servidor.red.ServidorRuleta
```

El servidor iniciará en el puerto 8000.

### Ejecución del Cliente

```bash
java cliente.ClienteRuleta
```

Conecta automáticamente a `localhost:8000`.

Para conexión remota, modificar línea 38 de `ClienteRuleta.java`:

```java
new ClienteRuleta("IP_SERVIDOR", 8000).IniciarCliente();
```

---

## Uso del Sistema

### Registro de Usuario

1. Ejecutar cliente
2. Seleccionar opción "2. Registrarse"
3. Introducir ID único
4. Establecer saldo inicial

### Inicio de Sesión

1. Ejecutar cliente
2. Seleccionar opción "1. Iniciar Sesion"
3. Introducir ID existente

### Realizar Apuestas

1. Seleccionar "2. Jugar" en menú principal
2. Esperar apertura de mesa si está cerrada
3. Presionar "1" para crear apuesta
4. Introducir cantidad (5€ - 10.000€)
5. Seleccionar tipo de apuesta
6. Especificar valor (número, color, etc.)
7. Confirmar envío

### Añadir Saldo

1. Seleccionar "1. Añadir saldo" en menú principal
2. Introducir cantidad (máximo 10.000€)
3. Confirmación automática

---

## Validaciones Implementadas

### Servidor

- Usuarios duplicados rechazados
- Saldo insuficiente bloquea apuesta
- Apuestas recibidas tras cierre se descartan
- Verificación de rango de números (0-36)
- Protección contra valores negativos

### Cliente

- ID no vacío en registro/login
- Saldo inicial numérico y positivo
- Cantidad de apuesta entre 5€ y 10.000€
- Formato correcto de valores (números, colores)
- Comprobación local de saldo antes de enviar

---

## Gestión de Errores

### Conexión

- Timeout de socket a 45 segundos
- Reconexión no permitida para mismo usuario
- Cierre ordenado con despedida
- Limpieza de recursos en finally

### Persistencia

- Creación automática de archivos XML
- Recuperación ante archivos corruptos
- Marshalling con validación desactivada
- Logs de errores sin interrupción del servicio

### Concurrencia

- Interrupción controlada de hilos
- Cancelación de futures no usados
- Protección contra BrokenBarrierException
- Restauración de estado de interrupción

---

## Limitaciones Conocidas

- Socket timeout fijo (45 segundos)
- Puerto hardcodeado (8000)
- Sin encriptación de comunicación
- Sin autenticación por contraseña
- Límite teórico de jugadores según pool
- Sin soporte para reconexión tras desconexión
- Historial XML crece indefinidamente

---

## Posibles Mejoras

- Implementar sistema de contraseñas
- Añadir apuestas combinadas
- Interfaz gráfica con JavaFX
- Base de datos SQL en lugar de XML
- Compresión de historial antiguo
- Sistema de niveles y rankings
- Chat entre jugadores
- Estadísticas personales
- Configuración externa (properties)
- Logger profesional (Log4j)

---

## Autor

Alberto Hidalgo Benito

---

## Licencia

Proyecto académico. Todos los derechos reservados.
