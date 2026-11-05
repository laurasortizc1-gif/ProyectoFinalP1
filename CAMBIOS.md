# Cambios realizados al Monedero Virtual

Resumen de lo que se agregó o corrigió para cumplir el enunciado del Proyecto Final.

## 1. Errores corregidos

| Problema | Solución |
|---|---|
| `TransaccionController.ejecutarTransaccion` capturaba `MontoNegativoException` en silencio, y la interfaz mostraba "Transacción realizada exitosamente" aunque fallara. | Ahora la excepción se propaga y la vista muestra el error real. |
| `pom.xml` apuntaba a `HelloApplication` (no existe), por lo que `mvn javafx:run` no arrancaba. | `mainClass` → `edu.co.uniquindio.poo.monedero.app.App`. |
| Las transacciones creadas desde `MonederoController` no tenían fecha y la tabla lanzaba `NullPointerException`. | La fecha y un id único se asignan en el constructor de `Transaccion`. |
| El tipo se guardaba como "Deposito" o "Depósito" según el constructor, lo que partía los reportes en dos grupos. | El tipo lo fija cada subclase. |
| `canjearPuntos` no hacía nada si faltaban puntos, y `Beneficio.aplicar` solo imprimía en consola. | Se lanza `PuntosInsuficientesException` y cada beneficio tiene un efecto real. |
| La transferencia no aparecía en el historial del receptor. | Ahora aparece como "Transferencia recibida". |
| No existían comisiones, así que los beneficios de rango ("tarifas reducidas") no tenían efecto. | Se crearon comisiones: retiro 0,5 % y transferencia 1 % (constantes en `Tarifas`). |

## 2. Funcionalidades nuevas

### Sistema de puntos (requisitos 1, 2 y 3)
- **`RegistroPuntos`** es el historial de puntos del cliente, con movimientos GANADOS, REVERTIDOS y CANJEADOS.
- **Reversión de transacciones.** `revertir()` es un método abstracto y cada subclase lo implementa a su manera (polimorfismo). `Cliente.recalcularPuntos()` recalcula los puntos con Streams a partir del historial, así que los puntos se actualizan dinámicamente.
- **Puntos acumulados y puntos disponibles.** Los acumulados definen el rango, así que canjear no baja de rango. Los disponibles son los que se gastan al canjear.
- **Beneficios.** `Beneficio` ahora es abstracta y tiene tres subclases:
  - `BeneficioDescuentoTransferencia` (100 pts): 10 % menos de comisión en transferencias durante 30 días.
  - `BeneficioRetirosSinCargo` (500 pts): un mes sin cargos por retiro.
  - `BeneficioBonoSaldo` (1000 pts): +50 en el monedero principal.
- **Rangos.** `Rango.calcularRango(puntos)` es la función que pide el enunciado. Cada rango da un multiplicador de puntos (x1,0 / x1,1 / x1,3 / x1,5) y un descuento en la comisión de transferencias (0 / 10 / 25 / 50 %).

### Transacciones programadas
- `TransaccionProgramada` guarda los datos de la operación, su frecuencia (`Frecuencia`: única, diaria, semanal o mensual) y la fecha de la próxima ejecución.
- `ProgramadorTransacciones` ordena las programadas con **Merge Sort** (`util/Ordenamiento`) por fecha de ejecución y ejecuta en ese orden las que ya vencieron. Si una transacción recurrente se atrasó varios periodos, se ejecuta una vez por cada periodo atrasado.
- `App` revisa cada 30 segundos, de forma automática, si hay programadas vencidas (usa un `Timeline`). También existe el botón "Procesar pendientes ahora".
- Cuando una programada falla (por ejemplo, por saldo insuficiente), se notifica al cliente y la programación sigue activa.

### Análisis de patrones de gasto (`AnalizadorGastos`, todo con Stream API)
El reporte incluye: gasto por tipo, gasto por mes y gasto por monedero (con `groupingBy`), estadísticas con `summaryStatistics`, el top 3 de gastos, el día de la semana con más gasto, la relación gasto/ingresos y el total de comisiones pagadas. En la interfaz se muestra un gráfico de barras del gasto mensual.

### Alertas y notificaciones
- `Notificacion` ahora es abstracta y tiene tres subclases: `NotificacionEmail` (Gmail), `NotificacionSMS` y `NotificacionWhatsApp`. Cada cliente elige su canal preferido.
- Se generan alertas por: saldo bajo (menos de $20.000), transacción sospechosa, recordatorio 24 h antes de una programada, transferencia recibida, canje de beneficio y reversión.
- El envío es **simulado**: la notificación queda en la bandeja del cliente (pestaña Notificaciones) y se imprime en consola. Para enviar de verdad haría falta integrar APIs externas como JavaMail o Twilio, con sus credenciales.

### Multimonedero
- Se pueden hacer transferencias entre monederos del mismo cliente, y no cobran comisión.
- Al transferir se puede elegir el monedero destino.
- Los datos de ejemplo incluyen a Alice con dos monederos: Monedero-Alice y Ahorros-Alice.

### Algoritmo de detección de transacciones atípicas (`DetectorTransaccionesAtipicas`)
**Justificación.** El comportamiento habitual del cliente se resume con dos medidas de su historial: el promedio (μ) y la desviación estándar poblacional (σ). Un monto mayor que μ + 2σ se aleja más de dos desviaciones de lo normal. Si los montos tuvieran una distribución aproximadamente normal, eso ocurriría en menos del 2,5 % de los casos. Por eso es un buen criterio para marcar una operación como potencialmente sospechosa.

**Funcionamiento:**
1. Se toman las transacciones que realizó el cliente en todos sus monederos, sin contar las revertidas (`flatMap` + `filter`).
2. Se calcula el promedio con `mapToDouble(...).average()`.
3. Se calcula la varianza con `mapToDouble(t -> (x-μ)²).average()`, y σ = √varianza.
4. Se calcula el umbral: μ + 2σ.
5. Se filtran las transacciones con monto mayor que el umbral, se marcan como sospechosas (`marcarSospechosa()`) y se genera una alerta.

El detector se usa de dos formas:
- **Automática:** cada transacción nueva se compara contra el historial anterior a ella.
- **Bajo demanda:** el botón "Detectar atípicas" analiza todo el historial.

Se exigen al menos 5 transacciones previas, porque con muy pocos datos la desviación no es representativa. Con desviación poblacional, un solo valor extremo solo puede superar 2σ cuando n ≥ 6.

**Complejidad temporal: O(n).** Se hacen tres recorridos lineales (promedio, varianza y filtro), es decir O(3n) = O(n). La complejidad espacial también es O(n), por la lista del historial.

**Merge Sort de las programadas:** O(n log n) en todos los casos y estable (si dos fechas son iguales, se respeta el orden de creación).

### Excepciones personalizadas nuevas
- `PuntosInsuficientesException`: al canjear sin puntos suficientes.
- `TransaccionNoReversibleException`: si la transacción ya fue revertida o si el destino ya no tiene saldo para devolver.
- `FechaProgramacionInvalidaException`: si la fecha es pasada o la hora tiene un formato inválido.

Se suman a las 10 que ya existían.

### Interfaz gráfica (JavaFX)
- **Banco:** nueva columna Rango y botón "Ranking por puntos".
- **Cliente:** ahora tiene pestañas:
  - Resumen y monederos: saldo total, puntos, rango y canal de notificación.
  - Puntos y beneficios: canje e historial de puntos.
  - Programadas: lista, procesar y cancelar.
  - Reportes y atípicas: reporte, gráfico y detección.
  - Notificaciones.
- **Monedero:** columnas de comisión, puntos y estado. Las filas sospechosas se resaltan en rojo. Botón para revertir una transacción.
- **Transacción:** selección del monedero destino, opción para programar (fecha, hora y frecuencia) y un resumen con la comisión y los puntos ganados.

## 3. Pruebas
Se agregó `FuncionalidadesAvanzadasTest` con 16 pruebas: puntos por tipo, límites de rangos, reversión, canjes, comisiones, multimonedero, detección de atípicas (incluido un caso con μ y σ conocidos), Merge Sort, orden de ejecución de las programadas, recurrencia, fecha inválida, reportes y alerta por WhatsApp.

Las 19 pruebas originales de `BancoTest` siguen pasando sin modificaciones (35/35 en total).

## 4. Archivos nuevos
```
model/   Frecuencia, CanalNotificacion, Tarifas, RegistroPuntos, TransaccionProgramada,
         BeneficioDescuentoTransferencia, BeneficioRetirosSinCargo, BeneficioBonoSaldo,
         NotificacionEmail, NotificacionSMS, NotificacionWhatsApp
service/ AnalizadorGastos, DetectorTransaccionesAtipicas, ProgramadorTransacciones, ServicioNotificaciones
util/    Ordenamiento (Merge Sort)
exceptions/ PuntosInsuficientes, TransaccionNoReversible, FechaProgramacionInvalida
test/    FuncionalidadesAvanzadasTest
```
